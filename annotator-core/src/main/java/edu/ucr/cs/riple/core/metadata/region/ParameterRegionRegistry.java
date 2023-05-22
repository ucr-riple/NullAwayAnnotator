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

package edu.ucr.cs.riple.core.metadata.region;

import edu.ucr.cs.riple.core.module.ModuleInfo;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.OnParameter;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Region registry for Method Parameters. This region registry can identify impacted regions for
 * fixes on {@link OnParameter}
 */
public class ParameterRegionRegistry implements RegionRegistry {

  /** ModuleInfo of the module which usage of parameters are stored. */
  private final ModuleInfo moduleInfo;
  /** {@link MethodRegionRegistry} instance, used to retrieve all sites. */
  private final MethodRegionRegistry methodRegionRegistry;

  public ParameterRegionRegistry(ModuleInfo moduleInfo, MethodRegionRegistry methodRegionRegistry) {
    this.moduleInfo = moduleInfo;
    this.methodRegionRegistry = methodRegionRegistry;
  }

  @Override
  public Optional<Set<Region>> getImpactedRegions(Location location) {
    if (!location.isOnParameter()) {
      return Optional.empty();
    }
    OnParameter parameter = location.toParameter();
    // Get regions which will be potentially affected by inheritance violations.
    Set<Region> regions =
        moduleInfo.getMethodRegistry().getImmediateSubMethods(parameter.toMethod()).stream()
            .map(node -> new Region(node.location.clazz, node.location.method))
            .collect(Collectors.toSet());
    // Add the method the fix is targeting.
    regions.add(new Region(parameter.clazz, parameter.enclosingMethod.method));
    // Add all call sites. It will also reserve call sites to prevent callers from passing @Nullable
    // simultaneously while investigating parameters impact.
    // See example below:
    // void foo(Object o) {
    //   bar(o);
    // }
    // void bar(Object o)
    //
    // We need to make sure that while investigating impact of `@Nullable` on bar#o, other callers
    // are not passing `@Nullable` to bar#o. Since the corresponding error will not be triggered
    // (passing `@Nullable` to `@Nonnull` parameter) as bar#o is temporarily annotated as @Nullable
    // to compute its impact.
    // See test: CoreTest#nestedParameters.
    regions.addAll(
        methodRegionRegistry.getCallersOfMethod(parameter.clazz, parameter.enclosingMethod.method));
    return Optional.of(regions);
  }
}
