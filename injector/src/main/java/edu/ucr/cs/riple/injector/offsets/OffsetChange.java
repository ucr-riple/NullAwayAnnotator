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

package edu.ucr.cs.riple.injector.offsets;

import java.util.List;

/**
 * Offset change information, stores the number of characters added / removed at a specific
 * position.
 */
public class OffsetChange {

  /** Index of addition / deletion. */
  public final int position;
  /** Number of characters added / removed. */
  public final int numChars;

  public OffsetChange(int position, int numChars) {
    this.position = position;
    this.numChars = numChars;
  }

  /**
   * Computes the original offset of the given offset according to existing offset changes.
   *
   * @param offset Given offset change.
   * @param existingOffsetChanges Existing offsets. The given list should be sorted by ascending by
   *     {@link OffsetChange#position}.
   * @return Original offset.
   */
  public static int getOriginalOffset(int offset, List<OffsetChange> existingOffsetChanges) {
    if (existingOffsetChanges == null) {
      return offset;
    }
    int result = offset;
    for (OffsetChange current : existingOffsetChanges) {
      if (result > current.position) {
        result -= current.numChars;
      }
      if (result < current.position) {
        break;
      }
    }
    return result;
  }

  /**
   * This method figures out what the offset change would be if the given offset changes were not
   * applied, and then returns the corresponding offset change instance. Mainly useful if this
   * change is created while other modifications are already applied to source code and the offset
   * according to original source code is desired.
   *
   * @param offsetChanges Given offset changes. The given list should be sorted by ascending by
   *     {@link OffsetChange#position}.
   * @return offset change with same {@link OffsetChange#numChars} and a position assuming the given
   *     list were not applied.
   */
  public OffsetChange getOffsetWithoutChanges(List<OffsetChange> offsetChanges) {
    return new OffsetChange(getOriginalOffset(this.position, offsetChanges), numChars);
  }
}
