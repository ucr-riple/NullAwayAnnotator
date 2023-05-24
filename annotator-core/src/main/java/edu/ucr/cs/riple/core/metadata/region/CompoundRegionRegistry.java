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

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.metadata.region.generatedcode.AnnotationProcessorHandler;
import edu.ucr.cs.riple.core.module.ModuleInfo;
import edu.ucr.cs.riple.injector.location.Location;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Container class for all region registries. This region registry can identify impacted regions for
 * all fix types.
 */
public class CompoundRegionRegistry implements RegionRegistry {

  /** List of all region registries. */
  private final ImmutableSet<RegionRegistry> registries;
  /** Module where this registry belongs to. */
  private final ModuleInfo moduleInfo;
  /**
   * Method region registry. This registry is used by other registries to identify impacted regions
   * specifically {@link AnnotationProcessorHandler}. To avoid recreating this instance, it is
   * stored here and passed to other registries.
   */
  private final MethodRegionRegistry methodRegionRegistry;

  public CompoundRegionRegistry(ModuleInfo moduleInfo) {
    this.moduleInfo = moduleInfo;
    this.methodRegionRegistry = new MethodRegionRegistry(moduleInfo);
    this.registries =
        ImmutableSet.of(
            new FieldRegionRegistry(moduleInfo),
            methodRegionRegistry,
            new LocalVariableRegionRegistry(),
            new ParameterRegionRegistry(moduleInfo, methodRegionRegistry));
  }

  @Override
  public Optional<Set<Region>> getImpactedRegions(Location location) {
    Set<Region> regions = new HashSet<>();
    this.registries.forEach(
        registry -> registry.getImpactedRegions(location).ifPresent(regions::addAll));
    this.moduleInfo
        .getAnnotationProcessorHandlers()
        .forEach(handler -> regions.addAll(handler.extendForGeneratedRegions(regions)));
    return Optional.of(regions);
  }

  /**
   * Returns the method region registry created by this instance.
   *
   * @return Method region registry instance.
   */
  public MethodRegionRegistry getMethodRegionRegistry() {
    return methodRegionRegistry;
  }
}
