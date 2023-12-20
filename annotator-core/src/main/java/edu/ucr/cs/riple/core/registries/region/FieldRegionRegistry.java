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
import edu.ucr.cs.riple.core.module.ModuleInfo;
import edu.ucr.cs.riple.core.registries.Registry;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.scanner.Serializer;
import java.util.stream.Collectors;

/**
 * Region registry for Fields. This region registry can identify impacted regions for fixes on
 * {@link OnField}.
 */
public class FieldRegionRegistry extends Registry<RegionRecord> implements RegionRegistry {

  /** ModuleInfo of the module which usages of fields are stored. */
  private final ModuleInfo moduleInfo;

  public FieldRegionRegistry(ModuleInfo moduleInfo) {
    super(
        moduleInfo.getModuleConfigurations().stream()
            .map(
                configuration ->
                    configuration.dir.resolve(Serializer.FIELD_IMPACTED_REGION_FILE_NAME))
            .collect(ImmutableSet.toImmutableSet()));
    this.moduleInfo = moduleInfo;
  }

  @Override
  protected Builder<RegionRecord> getBuilder() {
    return Utility::deserializeImpactedRegionRecord;
  }

  @Override
  public ImmutableSet<Region> getImpactedRegions(Location location) {
    if (!location.isOnField()) {
      return ImmutableSet.of();
    }
    OnField field = location.toField();
    // Add all regions where the field is assigned a new value or read.
    ImmutableSet.Builder<Region> builder = ImmutableSet.builder();
    builder.addAll(getImpactedRegionsByUse(location));
    // Add each a region for each field variable declared in the declaration statement.
    builder.addAll(
        field.variables.stream()
            .map(fieldName -> new Region(field.clazz, fieldName))
            .collect(Collectors.toSet()));
    // Check if field is initialized at declaration.
    if (moduleInfo.getFieldRegistry().isUninitializedField(field)) {
      // If not, add all constructors for the class.
      builder.addAll(
          moduleInfo.getMethodRegistry().getConstructorsForClass(field.clazz).stream()
              .map(onMethod -> new Region(onMethod.clazz, onMethod.method))
              .collect(Collectors.toSet()));
    }
    return builder.build();
  }

  @Override
  public ImmutableSet<Region> getImpactedRegionsByUse(Location location) {
    if (!location.isOnField()) {
      return ImmutableSet.of();
    }
    OnField field = location.toField();
    return findRecordsWithHashHint(
            candidate ->
                candidate.calleeClass.equals(field.clazz)
                    && field.isOnFieldWithName(candidate.calleeMember),
            RegionRecord.hash(field.clazz))
        .map(regionRecord -> regionRecord.region)
        .collect(ImmutableSet.toImmutableSet());
  }
}
