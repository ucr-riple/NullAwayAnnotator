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
import edu.ucr.cs.riple.core.explorers.DownStreamDependencyExplorer;
import edu.ucr.cs.riple.core.explorers.ExhaustiveExplorer;
import edu.ucr.cs.riple.core.explorers.Explorer;
import edu.ucr.cs.riple.core.explorers.OptimizedExplorer;
import edu.ucr.cs.riple.core.injectors.AnnotationInjector;
import edu.ucr.cs.riple.core.injectors.PhysicalInjector;
import edu.ucr.cs.riple.core.metadata.field.FieldDeclarationAnalysis;
import edu.ucr.cs.riple.core.metadata.field.FieldInitializationAnalysis;
import edu.ucr.cs.riple.core.metadata.index.Bank;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.method.MethodInheritanceTree;
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

public class Annotator {

  private final AnnotationInjector injector;
  private final Config config;
  // Set does not have get method, here we use map which retrieves elements efficiently.
  public final HashMap<Fix, Report> reports;

  public Annotator(Config config) {
    this.config = config;
    this.reports = new HashMap<>();
    this.injector = new PhysicalInjector(config);
  }

  public void start() {
    preprocess();
    long timer = config.log.startTimer();
    explore();
    config.log.stopTimerAndCapture(timer);
    Utility.writeLog(config);
  }

  private void preprocess() {
    System.out.println("Preprocessing...");
    Utility.setScannerCheckerActivation(config.target, true);
    this.reports.clear();
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

  private void explore() {
    Utility.setScannerCheckerActivation(config.target, true);
    Utility.buildTarget(config);
    Utility.setScannerCheckerActivation(config.target, false);
    FieldDeclarationAnalysis fieldDeclarationAnalysis = new FieldDeclarationAnalysis(config.target);
    MethodInheritanceTree tree =
        new MethodInheritanceTree(config.target.dir.resolve(Serializer.METHOD_INFO_FILE_NAME));
    // downStreamDependencyExplorer analyzes effects of all public APIs on downstream dependencies.
    // Through iterations, since the source code for downstream dependencies does not change and the
    // computation does not depend on the changes in the target module, it will compute the same
    // result in each iteration, therefore we perform the analysis only once and reuse it in each
    // iteration.
    DownStreamDependencyExplorer downStreamDependencyExplorer =
        new DownStreamDependencyExplorer(config, tree);
    if (config.downStreamDependenciesAnalysisActivated) {
      downStreamDependencyExplorer.explore();
    }
    while (true) {
      Utility.buildTarget(config);
      ImmutableSet<Fix> fixes =
          Utility.readFixesFromOutputDirectory(
                  config.target, Fix.factory(config, fieldDeclarationAnalysis))
              .filter(fix -> !config.useCache || !reports.containsKey(fix))
              .collect(ImmutableSet.toImmutableSet());
      Bank<Error> errorBank = new Bank<>(config.target.dir.resolve("errors.tsv"), Error::new);
      Bank<Fix> fixBank =
          new Bank<>(
              config.target.dir.resolve("fixes.tsv"),
              Fix.factory(config, fieldDeclarationAnalysis));
      tree = new MethodInheritanceTree(config.target.dir.resolve(Serializer.METHOD_INFO_FILE_NAME));
      RegionTracker tracker = new CompoundTracker(config.target, tree);
      Explorer explorer =
          config.exhaustiveSearch
              ? new ExhaustiveExplorer(injector, errorBank, fixBank, fixes, tree, config)
              : config.optimized
                  ? new OptimizedExplorer(
                      injector, errorBank, fixBank, tracker, fixes, tree, config.depth, config)
                  : new BasicExplorer(injector, errorBank, fixBank, fixes, tree, config);
      ImmutableSet<Report> latestReports = explorer.explore();
      int sizeBefore = reports.size();
      latestReports.forEach(
          report -> {
            if (config.downStreamDependenciesAnalysisActivated) {
              report.effect =
                  report.effect
                      + downStreamDependencyExplorer.effectOnDownstreamDependencies(report.root);
            }
            reports.putIfAbsent(report.root, report);
            reports.get(report.root).effect = report.effect;
            reports.get(report.root).finished = report.finished;
            reports.get(report.root).tree = report.tree;
            reports.get(report.root).triggered = report.triggered;
          });
      injector.injectFixes(
          latestReports.stream()
              .filter(report -> report.effect < 1)
              .flatMap(report -> config.chain ? report.tree.stream() : Stream.of(report.root))
              .collect(Collectors.toSet()));
      if (sizeBefore == this.reports.size()) {
        System.out.println("\nFinished annotating.");
        Utility.writeReports(
            config, reports.values().stream().collect(ImmutableSet.toImmutableSet()));
        return;
      }
      if (config.disableOuterLoop) {
        return;
      }
    }
  }
}
