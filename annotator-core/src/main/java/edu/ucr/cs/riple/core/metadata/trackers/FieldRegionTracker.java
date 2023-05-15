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

package edu.ucr.cs.riple.core.metadata.trackers;

import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.ModuleInfo;
import edu.ucr.cs.riple.core.metadata.Registry;
import edu.ucr.cs.riple.core.metadata.field.FieldRegistry;
import edu.ucr.cs.riple.core.metadata.method.MethodRegistry;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.scanner.Serializer;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Tracker for Fields. */
public class FieldRegionTracker extends Registry<TrackerNode> implements RegionTracker {

  /**
   * Store for field declarations. This is used to determine if a field is initialized at
   * declaration.
   */
  private final FieldRegistry fieldRegistry;
  /** The method registry. Used to retrieve constructors for a class */
  private final MethodRegistry methodRegistry;

  public FieldRegionTracker(
      Config config, ModuleInfo info, FieldRegistry fieldRegistry, MethodRegistry methodRegistry) {
    super(config, info.dir.resolve(Serializer.FIELD_GRAPH_FILE_NAME));
    this.fieldRegistry = fieldRegistry;
    this.methodRegistry = methodRegistry;
  }

  @Override
  protected Builder<TrackerNode> getBuilder() {
    return Utility::deserializeTrackerNode;
  }

  @Override
  public Optional<Set<Region>> getRegions(Location location) {
    if (!location.isOnField()) {
      return Optional.empty();
    }
    OnField field = location.toField();
    // Add all regions where the field is assigned a new value or read.
    Set<Region> ans =
        findRecordsWithHashHint(
                candidate ->
                    candidate.calleeClass.equals(field.clazz)
                        && field.isOnFieldWithName(candidate.calleeMember),
                TrackerNode.hash(field.clazz))
            .map(trackerNode -> trackerNode.region)
            .collect(Collectors.toSet());
    // Add each a region for each field variable declared in the declaration statement.
    ans.addAll(
        field.variables.stream()
            .map(fieldName -> new Region(field.clazz, fieldName))
            .collect(Collectors.toSet()));
    // Check if field is initialized at declaration.
    if (fieldRegistry.isUninitializedField(field)) {
      // If not, add all constructors for the class.
      ans.addAll(
          methodRegistry.getConstructorsForClass(field.clazz).stream()
              .map(onMethod -> new Region(onMethod.clazz, onMethod.method))
              .collect(Collectors.toSet()));
    }
    return Optional.of(ans);
  }
}
