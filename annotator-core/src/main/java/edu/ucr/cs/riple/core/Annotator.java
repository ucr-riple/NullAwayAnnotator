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
import edu.ucr.cs.riple.core.cache.TargetModuleCache;
import edu.ucr.cs.riple.core.cache.downstream.DownstreamImpactCache;
import edu.ucr.cs.riple.core.cache.downstream.DownstreamImpactCacheImpl;
import edu.ucr.cs.riple.core.cache.downstream.VoidDownstreamImpactCache;
import edu.ucr.cs.riple.core.evaluators.BasicEvaluator;
import edu.ucr.cs.riple.core.evaluators.CachedEvaluator;
import edu.ucr.cs.riple.core.evaluators.Evaluator;
import edu.ucr.cs.riple.core.evaluators.VoidEvaluator;
import edu.ucr.cs.riple.core.evaluators.suppliers.Supplier;
import edu.ucr.cs.riple.core.evaluators.suppliers.TargetModuleSupplier;
import edu.ucr.cs.riple.core.registries.index.Fix;
import edu.ucr.cs.riple.core.util.Utility;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The main class of the core module. Responsible for analyzing the target module and injecting the
 * corresponding annotations.
 */
public class Annotator {

  /** Annotator context. */
  public final Context context;

  /** Annotator configuration. */
  public final Config config;

  /**
   * Target module cache instance. Used to store the impact of fixes on the target module to avoid
   * redundant computations and infinite loops.
   */
  public final TargetModuleCache targetModuleCache;

  /**
   * Downstream impact cache instance. Used to store the impact of fixes on the downstream
   * dependencies. The downstream impact cache stores the impact of making each public API @Nullable
   * on downstream dependencies. downstreamImpactCache analyzes effects of all public APIs on
   * downstream dependencies. Through iterations, since the source code for downstream dependencies
   * does not change and the computation does not depend on the changes in the target module, it
   * will compute the same result in each iteration, therefore we perform the analysis only once and
   * reuse it in each iteration.
   */
  public final DownstreamImpactCache downstreamImpactCache;

  public Annotator(Config config) {
    this.config = config;
    this.context = new Context(config);
    this.targetModuleCache = new TargetModuleCache();
    this.downstreamImpactCache =
        config.downStreamDependenciesAnalysisActivated
            ? new DownstreamImpactCacheImpl(context)
            : new VoidDownstreamImpactCache();
  }

  /** Starts the annotating process consist of preprocess followed by the "annotate" phase. */
  public void start() {
    this.preprocess();
    long timer = context.log.startTimer();
    this.annotate();
    this.context.log.stopTimerAndCapture(timer);
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
    System.out.println("Annotating..." + config.inferenceActivated);
    downstreamImpactCache.analyzeDownstreamDependencies();
    ReportCache cache = context.reportCache;
    if (config.inferenceActivated) {
      // Outer loop starts.
      while (cache.isUpdated()) {
        executeNextIteration();
        if (config.disableOuterLoop) {
          break;
        }
      }
      // Perform once last iteration including all fixes.
      if (!config.disableOuterLoop) {
        cache.disable();
        executeNextIteration();
        cache.enable();
      }
    }
    if (config.resolveRemainingErrorMode.isSuppression()) {
      context.checker.suppressRemainingErrors();
    }
    if (config.resolveRemainingErrorMode.isResolution()) {
      context.checker.resolveRemainingErrors();
    }
    System.out.println("\nFinished annotating.");
    Utility.writeReports(context, cache.reports().stream().collect(ImmutableSet.toImmutableSet()));
  }

  /** Performs single iteration of inference/injection. */
  private void executeNextIteration() {
    ImmutableSet<Report> latestReports = processTriggeredFixes();
    // Compute boundaries of effects on downstream dependencies.
    latestReports.forEach(
        report -> {
          if (config.downStreamDependenciesAnalysisActivated) {
            report.computeBoundariesOfEffectivenessOnDownstreamDependencies(downstreamImpactCache);
          }
        });
    // Update cached reports store.
    context.reportCache.update(latestReports);
    // Tag reports according to selected analysis mode.
    config.mode.tag(downstreamImpactCache, latestReports);
    // Inject approved fixes.
    Set<Fix> selectedFixes =
        latestReports.stream()
            .filter(Report::approved)
            .flatMap(report -> config.chain ? report.tree.stream() : Stream.of(report.root))
            .collect(Collectors.toSet());
    context.getInjector().injectFixes(selectedFixes);
    // Update log.
    context.log.updateInjectedAnnotations(
        selectedFixes.stream().flatMap(fix -> fix.changes.stream()).collect(Collectors.toSet()));
    // Update impact saved state.
    downstreamImpactCache.updateImpactsAfterInjection(selectedFixes);
    targetModuleCache.updateImpactsAfterInjection(selectedFixes);
  }

  /**
   * Processes triggered fixes.
   *
   * @return Immutable set of reports from the triggered fixes.
   */
  public ImmutableSet<Report> processTriggeredFixes() {
    Utility.buildTarget(context);
    // Suggested fixes of target at the current state.
    ImmutableSet<Fix> fixes =
        Utility.readFixesFromOutputDirectory(context, context.targetModuleInfo).stream()
            .filter(fix -> !context.reportCache.processedFix(fix))
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
      return new CachedEvaluator(supplier);
    }
    return new BasicEvaluator(supplier);
  }
}
