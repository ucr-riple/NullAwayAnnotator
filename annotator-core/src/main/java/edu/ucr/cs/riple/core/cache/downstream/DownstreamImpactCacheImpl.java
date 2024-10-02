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

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.Context;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.cache.BaseCache;
import edu.ucr.cs.riple.core.evaluators.suppliers.DownstreamDependencySupplier;
import edu.ucr.cs.riple.core.module.ModuleInfo;
import edu.ucr.cs.riple.core.registries.index.Error;
import edu.ucr.cs.riple.core.registries.index.Fix;
import edu.ucr.cs.riple.core.registries.method.MethodRecord;
import edu.ucr.cs.riple.core.registries.region.FieldRegionRegistry;
import edu.ucr.cs.riple.core.registries.region.MethodRegionRegistry;
import edu.ucr.cs.riple.injector.changes.AddMarkerAnnotation;
import edu.ucr.cs.riple.injector.location.Location;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * Implementation for {@link DownstreamImpactCache} interface. This cache state is immutable and
 * once created, cannot be updated.
 */
public class DownstreamImpactCacheImpl
    extends BaseCache<DownstreamImpact, Map<Set<Location>, DownstreamImpact>>
    implements DownstreamImpactCache {

  /** Annotator context instance. */
  private final Context context;

  /**
   * Constructor for creating downstream impact cache. It populates the registry with a downstream
   * impact listing 0 triggered errors and 0 downstream fixes for the result of adding
   * {@code @Nullable} to each method in the target being annotated. The actual impacts, will be
   * computed once {@link #analyzeDownstreamDependencies()} is called.
   *
   * @param context Annotator context.
   */
  public DownstreamImpactCacheImpl(Context context) {
    super(new HashMap<>());
    this.context = context;
  }

  /**
   * Retrieves the set of locations that impact of making them {@code @Nullable} should be computed
   * on downstream dependencies and stored in this cache.
   *
   * @param context Annotator context.
   * @param moduleInfo Module info of the downstream dependencies. Downstream dependencies are
   *     collectively viewed as a single module.
   * @return Set of locations that impact of making them {@code @Nullable} should be computed on
   *     downstream dependencies and stored in this cache.
   */
  private ImmutableSet<Location> retrieveLocationsToCacheImpactsOnDownstreamDependencies(
      Context context, ModuleInfo moduleInfo) {
    ImmutableSet.Builder<Location> locationsToCache = ImmutableSet.builder();
    // Used to collect callers of each method.
    MethodRegionRegistry methodRegionRegistry = new MethodRegionRegistry(moduleInfo, context);
    FieldRegionRegistry fieldRegionRegistry = new FieldRegionRegistry(moduleInfo, context);
    // Collect public methods with non-primitive return types.
    locationsToCache.addAll(
        context
            .targetModuleInfo
            .getMethodRegistry()
            .getPublicMethodsWithNonPrimitivesReturn()
            .stream()
            .map(MethodRecord::toLocation)
            .filter(
                input ->
                    !methodRegionRegistry
                        .getImpactedRegionsByUse(input.toMethod())
                        // skip methods that are not called anywhere. This has a significant impact
                        // on performance.
                        .isEmpty())
            .collect(Collectors.toSet()));
    // Collect public fields with non-primitive types.
    locationsToCache.addAll(
        context.targetModuleInfo.getFieldRegistry().getPublicFieldWithNonPrimitiveType().stream()
            // skip fields that are not accessed anywhere. This has a significant impact
            // on performance.
            .filter(onField -> !fieldRegionRegistry.getImpactedRegionsByUse(onField).isEmpty())
            .collect(Collectors.toSet()));
    return locationsToCache.build();
  }

  @Override
  public void analyzeDownstreamDependencies() {
    System.out.println("Analyzing downstream dependencies...");
    DownstreamDependencySupplier supplier = new DownstreamDependencySupplier(context);
    // Generate fixes corresponding methods.
    ImmutableSet<Fix> fixes =
        retrieveLocationsToCacheImpactsOnDownstreamDependencies(context, supplier.getModuleInfo())
            .stream()
            .map(
                location ->
                    new Fix(
                        new AddMarkerAnnotation(location, context.config.nullableAnnot),
                        "null",
                        false))
            .collect(ImmutableSet.toImmutableSet());
    DownstreamImpactEvaluator evaluator = new DownstreamImpactEvaluator(supplier);
    ImmutableSet<Report> reports = evaluator.evaluate(fixes);
    // Update method status based on the results.
    reports.forEach(
        report -> {
          DownstreamImpact impact = new DownstreamImpact(report);
          store.put(report.root.toLocations(), impact);
        });
    System.out.println("Analyzing downstream dependencies completed!");
  }

  /**
   * Retrieves the corresponding {@link DownstreamImpact} to a fix. The fix should be targeting a
   * method or a field.
   *
   * @param fix Target fix.
   * @return Corresponding {@link DownstreamImpact}, null if not located.
   */
  @Nullable
  @Override
  public DownstreamImpact fetchImpact(Fix fix) {
    if (!(fix.isOnMethod() || fix.isOnField())) {
      // we currently store only impacts of fixes for methods / fields on downstream dependencies.
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
    if (lowerBoundEffectOfChainOptional.isEmpty()) {
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
    return getTriggeredErrors(fix).stream().anyMatch(error -> !error.isFixableOnTarget(context));
  }

  @Override
  public ImmutableSet<Error> getTriggeredErrorsForCollection(Collection<Fix> fixTree) {
    return fixTree.stream()
        .flatMap(
            fix -> {
              DownstreamImpact impact = fetchImpact(fix);
              return impact == null ? Stream.of() : impact.getTriggeredErrors().stream();
            })
        .collect(ImmutableSet.toImmutableSet());
  }

  @Override
  public ImmutableSet<Error> getTriggeredErrors(Fix fix) {
    // We currently only store impact of methods / fields on downstream dependencies.
    if (!(fix.isOnMethod() || fix.isOnField())) {
      return ImmutableSet.of();
    }
    DownstreamImpact impact = fetchImpact(fix);
    if (impact == null) {
      return ImmutableSet.of();
    }
    return ImmutableSet.copyOf(impact.getTriggeredErrors());
  }
}
