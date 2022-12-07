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

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.ModuleInfo;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.evaluators.suppliers.DownstreamDependencySupplier;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.method.MethodDeclarationTree;
import edu.ucr.cs.riple.core.metadata.trackers.MethodRegionTracker;
import edu.ucr.cs.riple.core.metadata.trackers.Region;
import edu.ucr.cs.riple.core.model.Impact;
import edu.ucr.cs.riple.core.model.StaticModel;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.changes.AddMarkerAnnotation;
import edu.ucr.cs.riple.injector.location.OnMethod;
import edu.ucr.cs.riple.injector.location.OnParameter;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;

/** Implementation for {@link GlobalModel} interface. */
public class GlobalModelImpl extends StaticModel<MethodImpact> implements GlobalModel {

  /** Set of downstream dependencies. */
  private final ImmutableSet<ModuleInfo> downstreamModules;

  public GlobalModelImpl(Config config, MethodDeclarationTree tree) {
    super(
        config,
        tree.getPublicMethodsWithNonPrimitivesReturn().stream()
            .map(
                methodNode ->
                    new MethodImpact(
                        new Fix(
                            new AddMarkerAnnotation(methodNode.location, config.nullableAnnot),
                            null,
                            null,
                            true)))
            .collect(toImmutableMap(Impact::toLocation, Function.identity())),
        tree);
    this.downstreamModules = config.downstreamInfo;
  }

  @Override
  public void analyzeDownstreamDependencies() {
    System.out.println("Analyzing downstream dependencies...");
    Utility.setScannerCheckerActivation(downstreamModules, true);
    Utility.buildDownstreamDependencies(config);
    Utility.setScannerCheckerActivation(downstreamModules, false);
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
                methodImpact ->
                    new Fix(
                        new AddMarkerAnnotation(
                            new OnMethod(
                                "null",
                                methodImpact.toMethod().clazz,
                                methodImpact.toMethod().method),
                            config.nullableAnnot),
                        "null",
                        new Region("null", "null"),
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
              Set<OnParameter> impactedParameters = evaluator.getImpactedParameters(method.fix);
              reports.stream()
                  .filter(input -> input.root.toMethod().equals(method.toMethod()))
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
  @Override
  public MethodImpact fetchImpact(Fix fix) {
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
    MethodImpact methodImpact = fetchImpact(fix);
    if (methodImpact == null) {
      return 0;
    }
    // Some triggered errors might be resolved due to fixes in the tree, and we should not double
    // count them.
    Set<Error> triggeredErrors = methodImpact.getTriggeredErrors();
    long resolvedErrors =
        triggeredErrors.stream().filter(error -> error.isResolvableWith(fixTree)).count();
    return triggeredErrors.size() - (int) resolvedErrors;
  }

  @Override
  public int computeLowerBoundOfNumberOfErrors(Set<Fix> tree) {
    OptionalInt lowerBoundEffectOfChainOptional =
        tree.stream().mapToInt(fix -> effectOnDownstreamDependencies(fix, tree)).max();
    if (lowerBoundEffectOfChainOptional.isEmpty()) {
      return 0;
    }
    return lowerBoundEffectOfChainOptional.getAsInt();
  }

  @Override
  public int computeUpperBoundOfNumberOfErrors(Set<Fix> tree) {
    return tree.stream().mapToInt(fix -> effectOnDownstreamDependencies(fix, tree)).sum();
  }
}
