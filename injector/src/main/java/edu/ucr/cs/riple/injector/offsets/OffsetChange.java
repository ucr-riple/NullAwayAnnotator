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
 * Offset change information, stores the number of character added / removed at a specific position.
 */
public class OffsetChange {

  /** Index of addition / deletion. */
  public final int position;
  /** Number of characters added / removed. */
  public final int dist;

  public OffsetChange(int position, int dist) {
    this.position = position;
    this.dist = dist;
  }

  /**
   * Translates the given offset change with the existing changes, to an offset where no other
   * offset exists.
   *
   * @param offset Given offset change.
   * @param existingOffsetChanges Existing offsets.
   * @return Original offset.
   */
  public static int translate(int offset, List<OffsetChange> existingOffsetChanges) {
    if (existingOffsetChanges == null) {
      return offset;
    }
    for (OffsetChange current : existingOffsetChanges) {
      if (offset > current.position) {
        offset -= current.dist;
      }
      if (offset < current.position) {
        break;
      }
    }
    return offset;
  }

  /**
   * Creates an offset relative to original source code.
   *
   * @param offsetChanges Existing offsets.
   * @return Original offset.
   */
  public OffsetChange relativeTo(List<OffsetChange> offsetChanges) {
    return new OffsetChange(translate(this.position, offsetChanges), dist);
  }
}