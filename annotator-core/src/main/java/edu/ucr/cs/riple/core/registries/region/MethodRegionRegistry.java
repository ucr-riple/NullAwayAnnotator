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

package edu.ucr.cs.riple.core.registries.region;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.Context;
import edu.ucr.cs.riple.core.module.ModuleInfo;
import edu.ucr.cs.riple.core.registries.Registry;
import edu.ucr.cs.riple.core.registries.method.MethodRecord;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.OnMethod;
import edu.ucr.cs.riple.scanner.Serializer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Region registry for Methods. This region registry can identify impacted regions for fixes on
 * {@link OnMethod}
 */
public class MethodRegionRegistry extends Registry<RegionRecord> implements RegionRegistry {

  /** ModuleInfo of the module which usage of methods are stored. */
  private final ModuleInfo moduleInfo;

  /**
   * A map from method to its callers. This relation is context insensitive and only contains direct
   * type based calls.
   */
  private final Map<MethodRecord, Set<MethodRecord>> callers;

  public MethodRegionRegistry(ModuleInfo moduleInfo, Context context) {
    super(
        moduleInfo.getModuleConfigurations().stream()
            .map(info -> info.dir.resolve(Serializer.METHOD_IMPACTED_REGION_FILE_NAME))
            .collect(ImmutableSet.toImmutableSet()),
        context);
    this.moduleInfo = moduleInfo;
    this.callers = new HashMap<>();
  }

  /**
   * Returns the set of callers of the given method.
   *
   * @param method Method to find its callers.
   * @return Set of callers of the given method.
   */
  public Set<MethodRecord> getCallers(MethodRecord method) {
    return callers.getOrDefault(method, new HashSet<>());
  }

  @Override
  public ImmutableSet<Region> getImpactedRegions(Location location) {
    if (!location.isOnMethod()) {
      return ImmutableSet.of();
    }
    ImmutableSet.Builder<Region> builder = ImmutableSet.builder();
    OnMethod onMethod = location.toMethod();
    // Add callers of method.
    builder.addAll(getImpactedRegionsByUse(onMethod));
    // Add method itself.
    builder.add(new Region(onMethod.clazz, onMethod.method));
    // Add immediate super method.
    MethodRecord parent = moduleInfo.getMethodRegistry().getImmediateSuperMethod(onMethod);
    if (parent != null && parent.isNonTop()) {
      builder.add(new Region(parent.location.clazz, parent.location.method));
    }
    return builder.build();
  }

  @Override
  public ImmutableSet<Region> getImpactedRegionsByUse(Location location) {
    if (!location.isOnMethod()) {
      return ImmutableSet.of();
    }
    OnMethod onMethod = location.toMethod();
    // Add callers of method.
    return findRecordsWithHashHint(
            candidate ->
                candidate.encClass.equals(onMethod.clazz)
                    && candidate.member.equals(onMethod.method),
            RegionRecord.hash(onMethod.clazz))
        .map(node -> node.region)
        .collect(ImmutableSet.toImmutableSet());
  }

  @Override
  protected Builder<RegionRecord> getBuilder() {
    return Utility::deserializeImpactedRegionRecord;
  }
}
