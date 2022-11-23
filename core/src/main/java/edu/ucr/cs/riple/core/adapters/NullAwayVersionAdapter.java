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

import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.trackers.Region;
import edu.ucr.cs.riple.core.metadata.trackers.TrackerNode;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.OnField;
import java.util.Set;

/**
 * Responsible for performing tasks related to NullAway / Type Annotator Scanner serialization
 * features.
 */
public interface NullAwayVersionAdapter {

  /**
   * Deserializes values produced by NullAway in a tsv file and creates a corresponding {@link Fix}
   * instance.
   *
   * @param location Location of the targeted element.
   * @param values Values in row of a TSV file.
   * @return Corresponding Fix instance with the passed values.
   */
  Fix deserializeFix(Location location, String[] values);

  /**
   * Deserializes values produced by NullAway in a tsv file and creates a corresponding {@link
   * Error} instance.
   *
   * @param values Values in row of a TSV file.
   * @return Corresponding Error instance with the passed values.
   */
  Error deserializeError(String[] values);

  /**
   * Deserializes values produced by Type Annotator Scanner in a tsv file and creates a
   * corresponding {@link TrackerNode} instance.
   *
   * @param values Values in row of a TSV file.
   * @return Corresponding TrackerNode instance with the passed values.
   */
  TrackerNode deserializeTrackerNode(String[] values);

  /**
   * Returns a set of regions enclosed by a field. Returns a set since there can multiple inline
   * field declarations.
   *
   * @param onField The target field.
   * @return Set of regions enclosed by the passed location.
   */
  Set<Region> getFieldRegionScope(OnField onField);
}
