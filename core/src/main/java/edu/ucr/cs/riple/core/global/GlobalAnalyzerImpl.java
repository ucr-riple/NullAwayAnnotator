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

package edu.ucr.cs.riple.core.global;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimaps;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.ModuleInfo;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.explorers.suppliers.DownstreamDependencySupplier;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/** Implementation for {@link GlobalAnalyzer} interface. */
public class GlobalAnalyzerImpl implements GlobalAnalyzer {

  /** Set of downstream dependencies. */
  private final ImmutableSet<ModuleInfo> downstreamModules;
  /** Public APIs in the target modules that have a non-primitive return value. */
  private final ImmutableMultimap<Integer, MethodImpact> methods;
  /** Annotator Config. */
  private final Config config;
  /** Method declaration tree instance. */
  private final MethodDeclarationTree tree;

  public GlobalAnalyzerImpl(Config config, MethodDeclarationTree tree) {
    this.config = config;
    this.downstreamModules = config.downstreamInfo;
    this.tree = tree;
    this.methods =
        Multimaps.index(
            tree.getPublicMethodsWithNonPrimitivesReturn().stream()
                .map(MethodImpact::new)
                .collect(ImmutableSet.toImmutableSet()),
            MethodImpact::hashCode);
  }

  @Override
  public void analyzeDownstreamDependencies() {
    System.out.println("Analyzing downstream dependencies...");
    Utility.setScannerCheckerActivation(downstreamModules, true);
    Utility.buildDownstreamDependencies(config);
    Utility.setScannerCheckerActivation(downstreamModules, false);
    // Collect callers of public APIs in module.
    MethodRegionTracker tracker = new MethodRegionTracker(config.downstreamInfo, tree);
    // Generate fixes corresponding methods.
    ImmutableSet<Fix> fixes =
        methods.values().stream()
            .filter(
                input ->
                    !tracker
                        .getCallersOfMethod(input.node.location.clazz, input.node.location.method)
                        .isEmpty()) // skip methods that are not called anywhere.
            .map(
                methodImpact ->
                    new Fix(
                        new AddMarkerAnnotation(
                            new OnMethod(
                                "null",
                                methodImpact.node.location.clazz,
                                methodImpact.node.location.method),
                            config.nullableAnnot),
                        "null",
                        "null",
                        "null",
                        false))
            .collect(ImmutableSet.toImmutableSet());
    DownstreamImpactExplorer analyzer =
        new DownstreamImpactExplorer(
            fixes, new DownstreamDependencySupplier(config, tree), tracker);
    ImmutableSet<Report> reports = analyzer.explore();
    // Update method status based on the results.
    methods
        .values()
        .forEach(
            method -> {
              MethodNode node = method.node;
              Set<OnParameter> impactedParameters = analyzer.getImpactedParameters(node.location);
              reports.stream()
                  .filter(input -> input.root.toMethod().equals(node.location))
                  .findAny()
                  .ifPresent(report -> method.setStatus(report, impactedParameters));
            });
    System.out.println("Analyzing downstream dependencies completed!");
  }

  /**
   * Retrieves the corresponding {@link MethodImpact} to a fix.
   *
   * @param fix Target fix.
   * @return Corresponding {@link MethodImpact}, null if not located.
   */
  @Nullable
  private MethodImpact fetchMethodImpactForFix(Fix fix) {
    if (!fix.isOnMethod()) {
      return null;
    }
    OnMethod onMethod = fix.toMethod();
    int predictedHash = MethodImpact.hash(onMethod.method, onMethod.clazz);
    Optional<MethodImpact> optional =
        this.methods.get(predictedHash).stream()
            .filter(m -> m.node.location.equals(onMethod))
            .findAny();
    return optional.orElse(null);
  }

  /**
   * Returns the effect of applying a fix on the target on downstream dependencies.
   *
   * @param fix Fix targeting an element in target.
   * @param fixesLocation Location in target that will be annotated as {@code @Nullable}.
   * @return Effect on downstream dependencies.
   */
  private int effectOnDownstreamDependencies(Fix fix, Set<Location> fixesLocation) {
    MethodImpact methodImpact = fetchMethodImpactForFix(fix);
    if (methodImpact == null) {
      return 0;
    }
    int individualEffect = methodImpact.getEffect();
    // Some triggered errors might be resolved due to fixes in the tree, and we should not double
    // count them.
    List<Error> triggeredErrors = methodImpact.getTriggeredErrors();
    long resolvedErrors =
        triggeredErrors.stream()
            .filter(error -> fixesLocation.contains(error.nonnullTarget))
            .count();
    return individualEffect - (int) resolvedErrors;
  }

  @Override
  public int computeLowerBoundOfNumberOfErrors(Set<Fix> tree) {
    Set<Location> fixLocations = tree.stream().map(Fix::toLocation).collect(Collectors.toSet());
    OptionalInt lowerBoundEffectOfChainOptional =
        tree.stream().mapToInt(fix -> effectOnDownstreamDependencies(fix, fixLocations)).max();
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
              MethodImpact impact = fetchMethodImpactForFix(fix);
              return impact == null ? Stream.of() : impact.getImpactedParameters().stream();
            })
        .collect(ImmutableSet.toImmutableSet());
  }

  @Override
  public List<Error> getTriggeredErrors(Fix fix) {
    // We currently only store impact of methods on downstream dependencies.
    if (!fix.isOnMethod()) {
      return Collections.emptyList();
    }
    MethodImpact impact = fetchMethodImpactForFix(fix);
    if (impact == null) {
      return Collections.emptyList();
    }
    return impact.getTriggeredErrors();
  }

  @Override
  public void updateImpactsAfterInjection(Set<Fix> fixes) {
    this.methods.values().forEach(methodImpact -> methodImpact.updateStatus(fixes));
  }

  @Override
  public boolean isNotFixableOnTarget(Fix fix) {
    // For unresolvable errors, nonnullTarget is initialized with location instance which all fields
    // are initialized to "null" string value. declaredInModule method in methodDeclarationTree
    // will return false for these locations. Hence, both the existence of fix and fix targeting an
    // element in target module is covered.
    return getTriggeredErrors(fix).stream()
        .anyMatch(error -> !tree.declaredInModule(error.nonnullTarget));
  }
}
