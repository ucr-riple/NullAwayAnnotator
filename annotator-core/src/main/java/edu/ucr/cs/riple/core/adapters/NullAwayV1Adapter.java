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

package edu.ucr.cs.riple.core.adapters;

import com.google.common.base.Preconditions;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.trackers.Region;
import edu.ucr.cs.riple.core.metadata.trackers.TrackerNode;
import edu.ucr.cs.riple.injector.changes.AddMarkerAnnotation;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.scanner.generatedcode.SourceType;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adapter working with versions below:
 *
 * <ul>
 *   <li>NullAway: Serialization version 1
 *   <li>Type Annotator Scanner: 1.3.4 or above
 * </ul>
 */
public class NullAwayV1Adapter implements NullAwayVersionAdapter {

  /** Annotator config. */
  private final Config config;

  public NullAwayV1Adapter(Config config) {
    this.config = config;
  }

  @Override
  public Fix deserializeFix(Location location, String[] values) {
    Preconditions.checkArgument(
        values.length == 10,
        "Expected 10 values to create Fix instance in NullAway serialization version 1 but found: "
            + values.length);
    Preconditions.checkArgument(
        values[7].equals("nullable"), "unsupported annotation: " + values[7]);
    return new Fix(
        new AddMarkerAnnotation(location, config.nullableAnnot),
        values[6],
        new Region(values[8], values[9]),
        true);
  }

  @Override
  public Error deserializeError(String[] values) {
    Preconditions.checkArgument(
        values.length == 10,
        "Expected 10 values to create Error instance in NullAway serialization version 1 but found: "
            + values.length);
    return new Error(
        values[0],
        values[1],
        new Region(values[2], values[3]),
        Location.createLocationFromArrayInfo(Arrays.copyOfRange(values, 4, 10)));
  }

  @Override
  public TrackerNode deserializeTrackerNode(String[] values) {
    Preconditions.checkArgument(
        values.length == 5,
        "Expected 5 values to create TrackerNode instance in NullAway serialization version 1 but found: "
            + values.length);
    return new TrackerNode(
        new Region(values[0], values[1], SourceType.valueOf(values[4])), values[2], values[3]);
  }

  @Override
  public Set<Region> getFieldRegionScope(OnField onField) {
    return onField.variables.stream()
        .map(fieldName -> new Region(onField.clazz, fieldName))
        .collect(Collectors.toSet());
  }
}
