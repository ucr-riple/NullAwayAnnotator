/*
 * MIT License
 *
 * Copyright (c) 2022 Nima Karimipour
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

package edu.ucr.cs.riple.core.cache.downstream;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimaps;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.ModuleInfo;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.evaluators.suppliers.DownstreamDependencySupplier;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.method.MethodDeclarationTree;
import edu.ucr.cs.riple.core.metadata.method.MethodNode;
import edu.ucr.cs.riple.core.metadata.trackers.MethodRegionTracker;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.changes.AddMarkerAnnotation;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.OnMethod;
import edu.ucr.cs.riple.injector.location.OnParameter;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/** Implementation for {@link DownstreamImpactCache} interface. */
public class DownstreamImpactCacheImpl implements DownstreamImpactCache {

  /** Set of downstream dependencies. */
  private final ImmutableSet<ModuleInfo> downstreamModules;
  /** Public APIs in the target modules that have a non-primitive return value. */
  private final ImmutableMultimap<Integer, DownstreamImpact> store;
  /** Annotator Config. */
  private final Config config;
  /** Method declaration tree instance. */
  private final MethodDeclarationTree tree;

  public DownstreamImpactCacheImpl(Config config, MethodDeclarationTree tree) {
    this.config = config;
    this.downstreamModules = config.downstreamInfo;
    this.tree = tree;
    this.store =
        Multimaps.index(
            tree.getPublicMethodsWithNonPrimitivesReturn().stream()
                .map(DownstreamImpact::new)
                .collect(ImmutableSet.toImmutableSet()),
            DownstreamImpact::hashCode);
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
                        .getCallersOfMethod(input.node.location.clazz, input.node.location.method)
                        .isEmpty()) // skip methods that are not called anywhere.
            .map(
                downstreamImpact ->
                    new Fix(
                        new AddMarkerAnnotation(
                            new OnMethod(
                                downstreamImpact.node.location.path,
                                downstreamImpact.node.location.clazz,
                                downstreamImpact.node.location.method),
                            config.nullableAnnot),
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
              MethodNode node = method.node;
              Set<OnParameter> impactedParameters = evaluator.getImpactedParameters(node.location);
              reports.stream()
                  .filter(input -> input.root.toMethod().equals(node.location))
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
  private DownstreamImpact fetchMethodImpactForFix(Fix fix) {
    if (!fix.isOnMethod()) {
      return null;
    }
    OnMethod onMethod = fix.toMethod();
    int predictedHash = DownstreamImpact.hash(onMethod.method, onMethod.clazz);
    Optional<DownstreamImpact> optional =
        this.store.get(predictedHash).stream()
            .filter(m -> m.node.location.equals(onMethod))
            .findAny();
    return optional.orElse(null);
  }

  /**
   * Returns the effect of applying a fix on the target on downstream dependencies.
   *
   * @param fix Fix targeting an element in target.
   * @param fixTree Location in target that will be annotated as {@code @Nullable}.
   * @return Effect on downstream dependencies.
   */
  private int effectOnDownstreamDependencies(Fix fix, Set<Location> fixTree) {
    DownstreamImpact downstreamImpact = fetchMethodImpactForFix(fix);
    if (downstreamImpact == null) {
      return 0;
    }
    int individualEffect = downstreamImpact.getEffect();
    // Some triggered errors might be resolved due to fixes in the tree, and we should not double
    // count them.
    Set<Error> triggeredErrors = downstreamImpact.getTriggeredErrors();
    long resolvedErrors =
        triggeredErrors.stream()
            .filter(error -> error.isSingleFix() && fixTree.contains(error.toResolvingLocation()))
            .count();
    return individualEffect - (int) resolvedErrors;
  }

  @Override
  public int computeLowerBoundOfNumberOfErrors(Set<Fix> tree) {
    Set<Location> fixTree = tree.stream().map(Fix::toLocation).collect(Collectors.toSet());
    OptionalInt lowerBoundEffectOfChainOptional =
        tree.stream().mapToInt(fix -> effectOnDownstreamDependencies(fix, fixTree)).max();
    if (lowerBoundEffectOfChainOptional.isEmpty()) {
      return 0;
    }
    return lowerBoundEffectOfChainOptional.getAsInt();
  }

  @Override
  public int computeUpperBoundOfNumberOfErrors(Set<Fix> tree) {
    Set<Location> fixesLocation = tree.stream().map(Fix::toLocation).collect(Collectors.toSet());
    return tree.stream().mapToInt(fix -> effectOnDownstreamDependencies(fix, fixesLocation)).sum();
  }

  @Override
  public ImmutableSet<OnParameter> getImpactedParameters(Set<Fix> fixTree) {
    return fixTree.stream()
        .filter(Fix::isOnMethod)
        .flatMap(
            fix -> {
              DownstreamImpact impact = fetchMethodImpactForFix(fix);
              return impact == null ? Stream.of() : impact.getImpactedParameters().stream();
            })
        .collect(ImmutableSet.toImmutableSet());
  }

  @Override
  public ImmutableSet<Error> getTriggeredErrors(Fix fix) {
    // We currently only store impact of methods on downstream dependencies.
    if (!fix.isOnMethod()) {
      return ImmutableSet.of();
    }
    DownstreamImpact impact = fetchMethodImpactForFix(fix);
    if (impact == null) {
      return ImmutableSet.of();
    }
    return ImmutableSet.copyOf(impact.getTriggeredErrors());
  }

  @Override
  public void updateImpactsAfterInjection(Set<Fix> fixes) {
    this.store.values().forEach(downstreamImpact -> downstreamImpact.updateStatus(fixes));
  }

  @Override
  public boolean isNotFixableOnTarget(Fix fix) {
    return getTriggeredErrors(fix).stream().anyMatch(error -> !error.isFixableOnTarget(tree));
  }
}
