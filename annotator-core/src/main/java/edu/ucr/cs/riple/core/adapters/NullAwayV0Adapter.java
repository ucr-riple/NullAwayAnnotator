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
import edu.ucr.cs.riple.core.metadata.field.FieldDeclarationStore;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.trackers.Region;
import edu.ucr.cs.riple.core.metadata.trackers.TrackerNode;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.OnField;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

/**
 * Adapter working with versions below:
 *
 * <ul>
 *   <li>NullAway: Serialization version 0
 *   <li>Type Annotator Scanner: 1.3.3 or below
 * </ul>
 */
public class NullAwayV0Adapter extends NullAwayAdapterBaseClass {

  /**
   * In this serialization version, offset of the errors is not serialized. Generally offsets are
   * used to distinguish errors. On this version of NullAway all serialized errors are treated as
   * unique errors among all errors. Therefore, we use this offset and increment it for each
   * serialized error to maintain this assumption.
   */
  private int offset;

  public NullAwayV0Adapter(Config config, FieldDeclarationStore fieldDeclarationStore) {
    super(config, fieldDeclarationStore, 0);
    this.offset = 0;
  }

  @Override
  public Error deserializeError(String[] values, FieldDeclarationStore store) {
    Preconditions.checkArgument(
        values.length == 10,
        "Expected 10 values to create Error instance in NullAway serialization version 0 but found: "
            + values.length);
    String encMember = !Region.getType(values[3]).equals(Region.Type.METHOD) ? "null" : values[3];
    Location nonnullTarget =
        Location.createLocationFromArrayInfo(Arrays.copyOfRange(values, 4, 10));
    String errorType = values[0];
    String errorMessage = values[1];
    Region region = new Region(values[2], encMember);
    // since we have no information of offset, we set all to zero.
    return createError(errorType, errorMessage, region, offset++, nonnullTarget, store);
  }

  @Override
  public TrackerNode deserializeTrackerNode(String[] values) {
    Preconditions.checkArgument(
        values.length == 4,
        "Expected 4 values to create TrackerNode instance in NullAway serialization version 0 but found: "
            + values.length);
    String encMember = !Region.getType(values[1]).equals(Region.Type.METHOD) ? "null" : values[1];
    return new TrackerNode(values[0], encMember, values[2], values[3]);
  }

  @Override
  public Set<Region> getFieldRegionScope(OnField onField) {
    return Collections.singleton(new Region(onField.clazz, "null"));
  }
}
