/*
 * MIT License
 *
 * Copyright (c) 2020 Nima Karimipour
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package edu.ucr.cs.riple.core;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.explorers.BasicExplorer;
import edu.ucr.cs.riple.core.explorers.ExhaustiveExplorer;
import edu.ucr.cs.riple.core.explorers.Explorer;
import edu.ucr.cs.riple.core.explorers.OptimizedExplorer;
import edu.ucr.cs.riple.core.explorers.suppliers.ExhaustiveSupplier;
import edu.ucr.cs.riple.core.explorers.suppliers.TargetModuleSupplier;
import edu.ucr.cs.riple.core.global.GlobalAnalyzer;
import edu.ucr.cs.riple.core.global.GlobalAnalyzerImpl;
import edu.ucr.cs.riple.core.global.NoOpGlobalAnalyzer;
import edu.ucr.cs.riple.core.injectors.AnnotationInjector;
import edu.ucr.cs.riple.core.injectors.PhysicalInjector;
import edu.ucr.cs.riple.core.metadata.field.FieldDeclarationAnalysis;
import edu.ucr.cs.riple.core.metadata.field.FieldInitializationAnalysis;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.method.MethodDeclarationTree;
import edu.ucr.cs.riple.core.metadata.trackers.CompoundTracker;
import edu.ucr.cs.riple.core.metadata.trackers.RegionTracker;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.changes.AddAnnotation;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.scanner.Serializer;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The main class of the core module. Responsible for analyzing the target module and injecting the
 * corresponding annotations.
 */
public class Annotator {

  /** Injector instance. */
  private final AnnotationInjector injector;
  /** Annotator config. */
  private final Config config;
  /**
   * Map of fix to their corresponding reports, used to detect processed fixes in a new batch of fix
   * suggestion. Note: Set does not have get method, here we use map which retrieves elements
   * efficiently.
   */
  public final HashMap<Fix, Report> cachedReports;

  public Annotator(Config config) {
    this.config = config;
    this.cachedReports = new HashMap<>();
    this.injector = new PhysicalInjector(config);
  }

  /** Starts the annotating process consist of preprocess followed by explore phase. */
  public void start() {
    preprocess();
    long timer = config.log.startTimer();
    explore();
    config.log.stopTimerAndCapture(timer);
    Utility.writeLog(config);
  }

  /**
   * Performs all the preprocessing tasks.
   *
   * <ul>
   *   <li>Performs the first build of the target module.
   *   <li>Detects uninitialized fields.
   *   <li>Detects initializer method candidates.
   *   <li>Marks selected initializer methods with {@code @Initializer} annotation.
   * </ul>
   */
  private void preprocess() {
    System.out.println("Preprocessing...");
    Utility.setScannerCheckerActivation(config.target, true);
    this.cachedReports.clear();
    System.out.println("Making the first build...");
    Utility.buildTarget(config, true);
    Set<OnField> uninitializedFields =
        Utility.readFixesFromOutputDirectory(config.target, Fix.factory(config, null))
            .filter(fix -> fix.isOnField() && fix.reasons.contains("FIELD_NO_INIT"))
            .map(Fix::toField)
            .collect(Collectors.toSet());
    FieldInitializationAnalysis analysis =
        new FieldInitializationAnalysis(config.target.dir.resolve("field_init.tsv"));
    Set<AddAnnotation> initializers =
        analysis
            .findInitializers(uninitializedFields)
            .map(onMethod -> new AddAnnotation(onMethod, config.initializerAnnot))
            .collect(Collectors.toSet());
    this.injector.injectAnnotations(initializers);
  }

  /** Performs iterations of inference/injection until no unseen fix is suggested. */
  private void explore() {
    Utility.setScannerCheckerActivation(config.target, true);
    Utility.buildTarget(config);
    Utility.setScannerCheckerActivation(config.target, false);
    FieldDeclarationAnalysis fieldDeclarationAnalysis = new FieldDeclarationAnalysis(config.target);
    MethodDeclarationTree tree =
        new MethodDeclarationTree(config.target.dir.resolve(Serializer.METHOD_INFO_FILE_NAME));
    // globalAnalyzer analyzes effects of all public APIs on downstream dependencies.
    // Through iterations, since the source code for downstream dependencies does not change and the
    // computation does not depend on the changes in the target module, it will compute the same
    // result in each iteration, therefore we perform the analysis only once and reuse it in each
    // iteration.
    GlobalAnalyzer globalAnalyzer =
        config.downStreamDependenciesAnalysisActivated
            ? new GlobalAnalyzerImpl(config, tree)
            : new NoOpGlobalAnalyzer();
    globalAnalyzer.analyzeDownstreamDependencies();
    boolean noNewFixTriggered = false;
    while (!noNewFixTriggered) {
      ImmutableSet<Report> latestReports =
          executeNextIteration(globalAnalyzer, fieldDeclarationAnalysis);
      int sizeBefore = cachedReports.size();
      // Update cached reports store.
      latestReports.forEach(
          report -> {
            if (config.downStreamDependenciesAnalysisActivated) {
              report.computeBoundariesOfEffectivenessOnDownstreamDependencies(globalAnalyzer);
            }
            cachedReports.putIfAbsent(report.root, report);
            Report cachedReport = cachedReports.get(report.root);
            cachedReport.localEffect = report.localEffect;
            cachedReport.finished = report.finished;
            cachedReport.tree = report.tree;
            cachedReport.triggeredFixes = report.triggeredFixes;
            cachedReport.triggeredErrors = report.triggeredErrors;
          });

      // Tag reports according to selected analysis mode.
      config.mode.tag(config, tree, globalAnalyzer, latestReports);

      // Inject approved fixes.
      Set<Fix> selectedFixes =
          latestReports.stream()
              .filter(Report::approved)
              .flatMap(report -> config.chain ? report.tree.stream() : Stream.of(report.root))
              .collect(Collectors.toSet());
      injector.injectFixes(selectedFixes);

      // Update impact saved state.
      globalAnalyzer.updateImpactsAfterInjection(selectedFixes);

      if (config.disableOuterLoop) {
        break;
      }
      noNewFixTriggered = sizeBefore == this.cachedReports.size();
    }
    System.out.println("\nFinished annotating.");
    Utility.writeReports(
        config, cachedReports.values().stream().collect(ImmutableSet.toImmutableSet()));
  }

  /**
   * Executes the next iteration of exploration.
   *
   * @param globalAnalyzer Global Analyzer instance.
   * @param fieldDeclarationAnalysis Field Declaration analysis to detect fixes on multiple inline
   *     field declaration statements.
   * @return Immutable set of reports from the executed iteration.
   */
  private ImmutableSet<Report> executeNextIteration(
      GlobalAnalyzer globalAnalyzer, FieldDeclarationAnalysis fieldDeclarationAnalysis) {
    Utility.buildTarget(config);
    // Suggested fixes of target at the current state.
    ImmutableSet<Fix> fixes =
        Utility.readFixesFromOutputDirectory(
                config.target, Fix.factory(config, fieldDeclarationAnalysis))
            .filter(fix -> !config.useCache || !cachedReports.containsKey(fix))
            .collect(ImmutableSet.toImmutableSet());

    // Initializing required explorer instances.
    MethodDeclarationTree tree =
        new MethodDeclarationTree(config.target.dir.resolve(Serializer.METHOD_INFO_FILE_NAME));
    RegionTracker tracker = new CompoundTracker(config.target, tree);
    TargetModuleSupplier supplier = new TargetModuleSupplier(config, tree);
    Explorer explorer =
        config.exhaustiveSearch
            ? new ExhaustiveExplorer(fixes, new ExhaustiveSupplier(config, tree))
            : config.optimized
                ? new OptimizedExplorer(fixes, supplier, globalAnalyzer, tracker)
                : new BasicExplorer(fixes, supplier, globalAnalyzer);
    // Result of the iteration analysis.
    return explorer.explore();
  }
}
