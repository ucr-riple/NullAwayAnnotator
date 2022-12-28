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
import edu.ucr.cs.riple.core.cache.downstream.DownstreamImpactCache;
import edu.ucr.cs.riple.core.cache.downstream.DownstreamImpactCacheImpl;
import edu.ucr.cs.riple.core.cache.downstream.VoidDownstreamImpactCache;
import edu.ucr.cs.riple.core.evaluators.BasicEvaluator;
import edu.ucr.cs.riple.core.evaluators.CachedEvaluator;
import edu.ucr.cs.riple.core.evaluators.Evaluator;
import edu.ucr.cs.riple.core.evaluators.VoidEvaluator;
import edu.ucr.cs.riple.core.evaluators.suppliers.Supplier;
import edu.ucr.cs.riple.core.evaluators.suppliers.TargetModuleSupplier;
import edu.ucr.cs.riple.core.injectors.AnnotationInjector;
import edu.ucr.cs.riple.core.injectors.PhysicalInjector;
import edu.ucr.cs.riple.core.metadata.field.FieldDeclarationStore;
import edu.ucr.cs.riple.core.metadata.field.FieldInitializationAnalysis;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.method.MethodDeclarationTree;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.changes.AddAnnotation;
import edu.ucr.cs.riple.injector.changes.AddMarkerAnnotation;
import edu.ucr.cs.riple.injector.changes.AddSingleElementAnnotation;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.injector.location.OnParameter;
import java.util.Objects;
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

  /** Reports cache. */
  public final ReportCache cache;

  private FieldDeclarationStore fieldDeclarationStore;
  private MethodDeclarationTree methodDeclarationTree;

  public Annotator(Config config) {
    this.config = config;
    this.cache = new ReportCache(config);
    this.injector = new PhysicalInjector(config);
  }

  /** Starts the annotating process consist of preprocess followed by the "annotate" phase. */
  public void start() {
    preprocess();
    long timer = config.log.startTimer();
    annotate();
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
    Utility.setScannerCheckerActivation(config, config.target, true);
    System.out.println("Making the first build...");
    Utility.buildTarget(config, true);
    fieldDeclarationStore = new FieldDeclarationStore(config, config.target);
    methodDeclarationTree = new MethodDeclarationTree(config);
    config.initializeAdapter(fieldDeclarationStore);
    Set<OnField> uninitializedFields =
        Utility.readFixesFromOutputDirectory(config, fieldDeclarationStore).stream()
            .filter(fix -> fix.isOnField() && fix.reasons.contains("FIELD_NO_INIT"))
            .map(Fix::toField)
            .collect(Collectors.toSet());
    FieldInitializationAnalysis analysis = new FieldInitializationAnalysis(config);
    Set<AddAnnotation> initializers =
        analysis
            .findInitializers(uninitializedFields)
            .map(onMethod -> new AddMarkerAnnotation(onMethod, config.initializerAnnot))
            .collect(Collectors.toSet());
    this.injector.injectAnnotations(initializers);
  }

  /** Performs iterations of inference/injection until no unseen fix is suggested. */
  private void annotate() {
    // Downstream impact cache stores impact of making all public APIs @Nullable on downstream
    // dependencies.
    // Through iterations, since the source code for downstream dependencies does not change and the
    // computation does not depend on the changes in the target module, it will compute the same
    // result in each iteration, therefore we perform the analysis only once and reuse it in each
    // iteration.
    DownstreamImpactCache downstreamImpactCache =
        config.downStreamDependenciesAnalysisActivated
            ? new DownstreamImpactCacheImpl(config, methodDeclarationTree)
            : new VoidDownstreamImpactCache();
    downstreamImpactCache.analyzeDownstreamDependencies();

    if (config.inferenceActivated) {
      // Outer loop starts.
      while (cache.isUpdated()) {
        executeNextIteration(downstreamImpactCache);
        if (config.disableOuterLoop) {
          break;
        }
      }

      // Perform once last iteration including all fixes.
      if (!config.disableOuterLoop) {
        cache.disable();
        executeNextIteration(downstreamImpactCache);
        cache.enable();
      }
    }

    if (config.forceResolveActivated) {
      forceResolveRemainingErrors();
    }

    System.out.println("\nFinished annotating.");
    Utility.writeReports(config, cache.reports().stream().collect(ImmutableSet.toImmutableSet()));
  }

  /**
   * Performs single iteration of inference/injection.
   *
   * @param downstreamImpactCache Downstream impact cache instance to detect impact of fixes outside
   *     of target module.
   */
  private void executeNextIteration(DownstreamImpactCache downstreamImpactCache) {
    ImmutableSet<Report> latestReports = processTriggeredFixes(downstreamImpactCache);
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
    config.mode.tag(config, downstreamImpactCache, latestReports);

    // Inject approved fixes.
    Set<Fix> selectedFixes =
        latestReports.stream()
            .filter(Report::approved)
            .flatMap(report -> config.chain ? report.tree.stream() : Stream.of(report.root))
            .collect(Collectors.toSet());
    injector.injectFixes(selectedFixes);

    // Update log.
    config.log.updateInjectedAnnotations(
        selectedFixes.stream().map(fix -> fix.change).collect(Collectors.toSet()));

    // Update impact saved state.
    downstreamImpactCache.updateImpactsAfterInjection(selectedFixes);
  }

  /**
   * Processes triggered fixes.
   *
   * @param downstreamImpactCache Downstream impact cache instance.
   * @return Immutable set of reports from the triggered fixes.
   */
  private ImmutableSet<Report> processTriggeredFixes(DownstreamImpactCache downstreamImpactCache) {
    Utility.buildTarget(config);
    // Suggested fixes of target at the current state.
    ImmutableSet<Fix> fixes =
        Utility.readFixesFromOutputDirectory(config, fieldDeclarationStore).stream()
            .filter(fix -> !cache.processedFix(fix))
            .collect(ImmutableSet.toImmutableSet());

    // Initializing required evaluator instances.
    TargetModuleSupplier supplier =
        new TargetModuleSupplier(config, downstreamImpactCache, methodDeclarationTree);
    Evaluator evaluator = getEvaluator(supplier);
    // Result of the iteration analysis.
    return evaluator.evaluate(fixes);
  }

  /**
   * Creates an {@link Evaluator} corresponding to config values.
   *
   * @param supplier Supplier to create an instance of Evaluator.
   * @return {@link Evaluator} corresponding to config values.
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

  /**
   * Resolves all remaining errors in target module by following steps below:
   *
   * <ul>
   *   <li>Enclosing method of triggered errors will be marked with {@code @NullUnmarked}
   *       annotation.
   *   <li>Uninitialized fields (inline or by constructor) will be annotated as
   *       {@code @SuppressWarnings("NullAway.Init")}.
   *   <li>Explicit {@code Nullable} assignments to fields will be annotated as
   *       {@code @SuppressWarnings("NullAway")}.
   * </ul>
   */
  private void forceResolveRemainingErrors() {
    // Collect regions with remaining errors.
    Utility.buildTarget(config);
    Set<Error> remainingErrors =
        Utility.readErrorsFromOutputDirectory(config, config.target, fieldDeclarationStore);
    Set<Fix> remainingFixes = Utility.readFixesFromOutputDirectory(config, fieldDeclarationStore);
    // Collect all regions for NullUnmarked.
    // For all errors in regions which correspond to a method's body, we can add @NullUnmarked at
    // the method level.
    Set<AddAnnotation> nullUnMarkedAnnotations =
        remainingErrors.stream()
            // find the corresponding method nodes.
            .map(
                error -> {
                  if (error.getRegion().isOnMethod()) {
                    return methodDeclarationTree.findNode(error.encMember(), error.encClass());
                  }
                  // For methods invoked in an initialization region, where the error is that
                  // `@Nullable` is being passed as an argument, we add a `@NullUnmarked` annotation
                  // to the called method.
                  if (error.messageType.equals("PASS_NULLABLE")
                      && error.isSingleFix()
                      && error.toResolvingLocation().isOnParameter()) {
                    OnParameter nullableParameter = error.toResolvingParameter();
                    return methodDeclarationTree.findNode(
                        nullableParameter.method, nullableParameter.clazz);
                  }
                  return null;
                })
            // Filter null values from map above.
            .filter(Objects::nonNull)
            .map(node -> new AddMarkerAnnotation(node.location, config.nullUnMarkedAnnotation))
            .collect(Collectors.toSet());
    injector.injectAnnotations(nullUnMarkedAnnotations);
    // Update log.
    config.log.updateInjectedAnnotations(nullUnMarkedAnnotations);

    // Collect suppress warnings, errors on field declaration regions.
    Set<OnField> fieldsWithSuppressWarnings =
        remainingErrors.stream()
            .filter(
                error -> {
                  if (!error.getRegion().isOnField()) {
                    return false;
                  }
                  if (error.messageType.equals("PASS_NULLABLE")) {
                    // It is already resolved with @NullUnmarked selected above.
                    return false;
                  }
                  // We can silence them by SuppressWarnings("NullAway.Init")
                  return !error.messageType.equals("METHOD_NO_INIT")
                      && !error.messageType.equals("FIELD_NO_INIT");
                })
            .map(
                error ->
                    fieldDeclarationStore.getLocationOnField(
                        error.getRegion().clazz, error.getRegion().member))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Set<AddAnnotation> suppressWarningsAnnotations =
        fieldsWithSuppressWarnings.stream()
            .map(
                onField ->
                    new AddSingleElementAnnotation(onField, "SuppressWarnings", "NullAway", false))
            .collect(Collectors.toSet());
    injector.injectAnnotations(suppressWarningsAnnotations);
    // Update log.
    config.log.updateInjectedAnnotations(suppressWarningsAnnotations);

    // Collect NullAway.Init SuppressWarnings
    Set<AddAnnotation> initializationSuppressWarningsAnnotations =
        remainingFixes.stream()
            .filter(
                fix ->
                    fix.isOnField()
                        && (fix.reasons.contains("METHOD_NO_INIT")
                            || fix.reasons.contains("FIELD_NO_INIT")))
            // Filter nodes annotated with SuppressWarnings("NullAway")
            .filter(fix -> !fieldsWithSuppressWarnings.contains(fix.toField()))
            .map(
                fix ->
                    new AddSingleElementAnnotation(
                        fix.toField(), "SuppressWarnings", "NullAway.Init", false))
            .collect(Collectors.toSet());
    injector.injectAnnotations(initializationSuppressWarningsAnnotations);
    // Update log.
    config.log.updateInjectedAnnotations(initializationSuppressWarningsAnnotations);
  }
}
