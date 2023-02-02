/*
 * MIT License
 *
 * Copyright (c) 2022 anonymous
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

package com.example.tool.core.cache.downstream;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.example.tool.core.Config;
import com.example.tool.core.ModuleInfo;
import com.example.tool.core.metadata.index.Error;
import com.example.tool.core.metadata.index.Fix;
import com.example.tool.core.metadata.method.MethodDeclarationTree;
import com.example.tool.core.metadata.trackers.MethodRegionTracker;
import com.example.tool.core.util.Utility;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.example.tool.core.Report;
import com.example.tool.core.cache.BaseCache;
import com.example.tool.core.cache.Impact;
import com.example.tool.core.evaluators.suppliers.DownstreamDependencySupplier;
import com.example.tool.injector.changes.AddMarkerAnnotation;
import com.example.tool.injector.location.Location;
import com.example.tool.injector.location.OnParameter;
import java.util.Collection;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * Implementation for {@link DownstreamImpactCache} interface. This cache state is immutable and
 * once created, cannot be updated.
 */
public class DownstreamImpactCacheImpl
    extends BaseCache<DownstreamImpact, ImmutableMap<Location, DownstreamImpact>>
    implements DownstreamImpactCache {

  /** Set of downstream dependencies. */
  private final ImmutableSet<ModuleInfo> downstreamModules;

  /**
   * Constructor for creating downstream impact cache. It initializes the cache with all the entries
   * they can have. The corresponding values for these cache entire will be computed and once {@link
   * DownstreamImpactCache#analyzeDownstreamDependencies()} is called.
   *
   * @param config Annotator config.
   * @param tree Method declaration tree for target module used to collect public methods with
   *     non-primitive return types to compute their impacts on downstream dependencies.
   */
  public DownstreamImpactCacheImpl(Config config, MethodDeclarationTree tree) {
    super(
        config,
        tree.getPublicMethodsWithNonPrimitivesReturn().stream()
            .map(
                methodNode ->
                    new DownstreamImpact(
                        new Fix(
                            new AddMarkerAnnotation(methodNode.location, config.nullableAnnot),
                            "null",
                            true)))
            .collect(toImmutableMap(Impact::toLocation, Function.identity())),
        tree);
    this.downstreamModules = config.downstreamInfo;
  }

  @Override
  public void analyzeDownstreamDependencies() {
    System.out.println("Analyzing downstream dependencies...");
    Utility.setScannerCheckerActivation(config, downstreamModules, true);
    Utility.buildDownstreamDependencies(config);
    Utility.setScannerCheckerActivation(config, downstreamModules, false);
    // Collect callers of public APIs in module.
    MethodRegionTracker tracker = new MethodRegionTracker(config, config.downstreamInfo, tree);
    // Generate fixes corresponding methods.
    ImmutableSet<Fix> fixes =
        store.values().stream()
            .filter(
                input ->
                    !tracker
                        .getCallersOfMethod(input.toMethod().clazz, input.toMethod().method)
                        .isEmpty()) // skip methods that are not called anywhere.
            .map(
                downstreamImpact ->
                    new Fix(
                        new AddMarkerAnnotation(downstreamImpact.toMethod(), config.nullableAnnot),
                        "null",
                        false))
            .collect(ImmutableSet.toImmutableSet());
    DownstreamImpactEvaluator evaluator =
        new DownstreamImpactEvaluator(new DownstreamDependencySupplier(config, tracker, tree));
    ImmutableSet<Report> reports = evaluator.evaluate(fixes);
    // Update method status based on the results.
    this.store
        .values()
        .forEach(
            method -> {
              Set<OnParameter> impactedParameters =
                  evaluator.getImpactedParameters(method.fix.toMethod());
              reports.stream()
                  .filter(input -> input.root.toMethod().equals(method.toMethod()))
                  .findAny()
                  .ifPresent(report -> method.setStatus(report, impactedParameters));
            });
    System.out.println("Analyzing downstream dependencies completed!");
  }

  /**
   * Retrieves the corresponding {@link DownstreamImpact} to a fix.
   *
   * @param fix Target fix.
   * @return Corresponding {@link DownstreamImpact}, null if not located.
   */
  @Nullable
  @Override
  public DownstreamImpact fetchImpact(Fix fix) {
    if (!fix.isOnMethod()) {
      // we currently store only impacts of fixes for methods on downstream dependencies.
      return null;
    }
    return super.fetchImpact(fix);
  }

  /**
   * Returns the effect of applying a fix on the target on downstream dependencies.
   *
   * @param fix Fix targeting an element in target.
   * @param fixTree Fix tree in target that will be annotated as {@code @Nullable}.
   * @return Effect on downstream dependencies.
   */
  private int effectOnDownstreamDependencies(Fix fix, Set<Fix> fixTree) {
    DownstreamImpact downstreamImpact = fetchImpact(fix);
    if (downstreamImpact == null) {
      return 0;
    }
    // Some triggered errors might be resolved due to fixes in the tree, and we should not double
    // count them.
    Set<Error> triggeredErrors = downstreamImpact.getTriggeredErrors();
    long resolvedErrors =
        triggeredErrors.stream().filter(error -> error.isResolvableWith(fixTree)).count();
    return triggeredErrors.size() - (int) resolvedErrors;
  }

  @Override
  public int computeLowerBoundOfNumberOfErrors(Set<Fix> tree) {
    OptionalInt lowerBoundEffectOfChainOptional =
        tree.stream().mapToInt(fix -> effectOnDownstreamDependencies(fix, tree)).max();
    if (!lowerBoundEffectOfChainOptional.isPresent()) {
      return 0;
    }
    return lowerBoundEffectOfChainOptional.getAsInt();
  }

  @Override
  public int computeUpperBoundOfNumberOfErrors(Set<Fix> tree) {
    return tree.stream().mapToInt(fix -> effectOnDownstreamDependencies(fix, tree)).sum();
  }

  @Override
  public boolean triggersUnresolvableErrorsOnDownstream(Fix fix) {
    return getTriggeredErrors(fix).stream().anyMatch(error -> !error.isFixableOnTarget(tree));
  }

  @Override
  public ImmutableSet<Error> getTriggeredErrorsForCollection(Collection<Fix> fixTree) {
    return fixTree.stream()
        .filter(Fix::isOnMethod)
        .flatMap(
            fix -> {
              DownstreamImpact impact = fetchImpact(fix);
              return impact == null ? Stream.of() : impact.getTriggeredErrors().stream();
            })
        .collect(ImmutableSet.toImmutableSet());
  }

  @Override
  public ImmutableSet<Error> getTriggeredErrors(Fix fix) {
    // We currently only store impact of methods on downstream dependencies.
    if (!fix.isOnMethod()) {
      return ImmutableSet.of();
    }
    DownstreamImpact impact = fetchImpact(fix);
    if (impact == null) {
      return ImmutableSet.of();
    }
    return ImmutableSet.copyOf(impact.getTriggeredErrors());
  }

  @Override
  public void updateImpactsAfterInjection(Collection<Fix> fixes) {
    this.store
        .values()
        .forEach(downstreamImpact -> downstreamImpact.updateStatusAfterInjection(fixes));
  }
}