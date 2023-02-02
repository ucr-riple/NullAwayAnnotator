/*
 * MIT License
 *
 * Copyright (c) 2022 anonymous
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

package com.example.tool.core.adapters;

import com.example.tool.core.metadata.field.FieldDeclarationStore;
import com.example.tool.core.metadata.index.Error;
import com.example.tool.core.metadata.trackers.Region;
import com.example.tool.core.metadata.trackers.TrackerNode;
import com.example.tool.injector.location.OnField;
import java.util.Set;

/**
 * Responsible for performing tasks related to NullAway / Type Annotator Scanner serialization
 * features.
 */
public interface NullAwayVersionAdapter {

  /**
   * Deserializes values produced by NullAway in a tsv file and creates a corresponding {@link
   * Error} instance.
   *
   * @param values Values in row of a TSV file.
   * @param store Field declaration store to generate the set of resolving fixes for the
   *     deserialized error.
   * @return Corresponding Error instance with the passed values.
   */
  Error deserializeError(String[] values, FieldDeclarationStore store);

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

  /**
   * Returns the serialization version number which this adapter is associated with.
   *
   * @return Serialization number.
   */
  int getVersionNumber();
}
