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

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.cache.TargetModuleCache;
import edu.ucr.cs.riple.core.cache.downstream.DownstreamImpactCache;
import edu.ucr.cs.riple.core.cache.downstream.VoidDownstreamImpactCache;
import edu.ucr.cs.riple.core.checkers.ucrtaint.UCRTaintError;
import edu.ucr.cs.riple.core.evaluators.BasicEvaluator;
import edu.ucr.cs.riple.core.evaluators.Evaluator;
import edu.ucr.cs.riple.core.evaluators.VoidEvaluator;
import edu.ucr.cs.riple.core.evaluators.suppliers.Supplier;
import edu.ucr.cs.riple.core.evaluators.suppliers.TargetModuleSupplier;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.changes.AddAnnotation;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * The main class of the core module. Responsible for analyzing the target module and injecting the
 * corresponding annotations.
 */
public class Annotator {

  /** Annotator context. */
  public final Context context;
  /** Reports cache. */
  public final ReportCache cache;
  /** Annotator configuration. */
  public final Config config;

  boolean addedMoreFixes = false;

  public Annotator(Config config) {
    this.config = config;
    this.context = new Context(config);
    this.cache = new ReportCache(config);
  }

  /** Starts the annotating process consist of preprocess followed by the "annotate" phase. */
  public void start() {
    ParserConfiguration parserConfiguration = new ParserConfiguration();
    parserConfiguration.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
    StaticJavaParser.setConfiguration(parserConfiguration);
    preprocess();
    long timer = context.log.startTimer();
    annotate();
    if (!config.disableCleanup) {
      context.checker.cleanup();
    }
    context.log.stopTimerAndCapture(timer);
    Utility.writeLog(context);
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
    context.checker.preprocess();
  }

  /** Performs iterations of inference/injection until no unseen fix is suggested. */
  private void annotate() {
    // The downstream impact cache stores the impact of making each public API @Nullable on
    // downstream dependencies.
    // downstreamImpactCache analyzes effects of all public APIs on downstream dependencies.
    // Through iterations, since the source code for downstream dependencies does not change and the
    // computation does not depend on the changes in the target module, it will compute the same
    // result in each iteration, therefore we perform the analysis only once and reuse it in each
    // iteration.
    DownstreamImpactCache downstreamImpactCache = new VoidDownstreamImpactCache();
    downstreamImpactCache.analyzeDownstreamDependencies();
    TargetModuleCache targetModuleCache = new TargetModuleCache();
    if (config.inferenceActivated) {
      System.out.println("Max Depth level: " + config.depth + ", chain: " + config.chain);
      // Outer loop starts.
      while (cache.isUpdated()) {
        executeNextIteration(targetModuleCache, downstreamImpactCache);
        if (config.disableOuterLoop) {
          break;
        }
      }
      // Perform once last iteration including all fixes.
      if (!config.disableOuterLoop) {
        cache.disable();
        System.out.println("Doing additional search...");
        System.out.println("Setting Chain from " + config.chain + " to true");
        config.chain = true;
        executeNextIteration(targetModuleCache, downstreamImpactCache);
        System.out.println("Doing final search...");
        int i = 0;
        while (addedMoreFixes && i < 3) {
          i++;
          System.out.println(i + " - Added fixes, looking for more fixes...");
          executeNextIteration(targetModuleCache, downstreamImpactCache);
        }
        cache.enable();
      }
    }
    if (config.suppressRemainingErrors) {
      context.checker.suppressRemainingErrors();
    }
    System.out.println("\nFinished annotating.");
    Utility.writeReports(context, cache.reports().stream().collect(ImmutableSet.toImmutableSet()));
  }

  /**
   * Performs single iteration of inference/injection.
   *
   * @param targetModuleCache Target impact cache instance.
   * @param downstreamImpactCache Downstream impact cache instance to retrieve impact of fixes on
   *     downstream dependencies.
   */
  private void executeNextIteration(
      TargetModuleCache targetModuleCache, DownstreamImpactCache downstreamImpactCache) {
    ImmutableSet<Report> latestReports =
        processTriggeredFixes(targetModuleCache, downstreamImpactCache);
    // Compute boundaries of effects on downstream dependencies.
    latestReports.forEach(
        report -> {
          if (config.downStreamDependenciesAnalysisActivated) {
            report.computeBoundariesOfEffectivenessOnDownstreamDependencies(downstreamImpactCache);
          }
        });
    // Update cached reports store.
    cache.update(latestReports);
    // Tag reports according to selected analysis mode.
    config.mode.tag(downstreamImpactCache, latestReports);
    // Inject approved fixes.
    Set<Fix> selectedFixes =
        latestReports.stream()
            .filter(Report::approved)
            .flatMap(report -> config.chain ? report.tree.stream() : report.root.stream())
            .collect(Collectors.toSet());


    // DEBUG
    if(Main.DEBUG){
      Set<String> injectedFixes =
          selectedFixes.stream().map(Fix::toString).collect(Collectors.toSet());
      StringBuilder logInjected = new StringBuilder("Injected fixes: \n");
      injectedFixes.forEach(logInjected::append);
      Utility.log(logInjected.toString());
    }

    context.injector.injectFixes(selectedFixes);
    addedMoreFixes = !selectedFixes.isEmpty();
    Set<AddAnnotation> addedAnnotations =
        selectedFixes.stream()
            .map(fix -> fix.change)
            .collect(Collectors.toSet());
    Set<String> annotationString = addedAnnotations.stream().map(Object::toString).collect(Collectors.toSet());
    StringBuilder log = new StringBuilder("Added annotations: \n");
    for (String s : annotationString) {
      log.append(s).append("\n");
    }
    Utility.log(log.toString());
    // Update log.
    context.log.updateInjectedAnnotations(addedAnnotations);
    // Update impact saved state.
    downstreamImpactCache.updateImpactsAfterInjection(selectedFixes);
    targetModuleCache.updateImpactsAfterInjection(selectedFixes);
  }

  /**
   * Processes triggered fixes.
   *
   * @param downstreamImpactCache Downstream impact cache instance.
   * @param targetModuleCache Target impact cache instance.
   * @return Immutable set of reports from the triggered fixes.
   */
  private ImmutableSet<Report> processTriggeredFixes(
      TargetModuleCache targetModuleCache, DownstreamImpactCache downstreamImpactCache) {
    Utility.buildTarget(context);
    Set<UCRTaintError> errors =
        Utility.readErrorsFromOutputDirectory(
            context, context.targetModuleInfo, UCRTaintError.class);
    System.out.println("Triggered Errors in this iteration: " + errors.size());
    // Suggested fixes of target at the current state.
    ImmutableSet<ImmutableSet<Fix>> fixes =
        Utility.readFixesFromOutputDirectory(context, context.targetModuleInfo).stream()
            .filter(fix -> !cache.processedFix(fix))
            .collect(ImmutableSet.toImmutableSet());
    // Initializing required evaluator instances.
    TargetModuleSupplier supplier =
        new TargetModuleSupplier(context, targetModuleCache, downstreamImpactCache);
    Evaluator evaluator = getEvaluator(supplier);
    // Result of the iteration analysis.
    return evaluator.evaluate(fixes);
  }

  /**
   * Creates an {@link Evaluator} corresponding to context values.
   *
   * @param supplier Supplier to create an instance of Evaluator.
   * @return {@link Evaluator} corresponding to context values.
   */
  private Evaluator getEvaluator(Supplier supplier) {
    if (config.exhaustiveSearch) {
      return new VoidEvaluator();
    }
    if (config.useImpactCache) {
      throw new RuntimeException("Impact cache is not supported in this version.");
    }
    return new BasicEvaluator(supplier);
  }
}
