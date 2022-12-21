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

package edu.ucr.cs.riple.core.metadata.trackers.generatedcode;

import edu.ucr.cs.riple.core.metadata.trackers.Region;
import java.util.Set;

/**
 * Interface for Trackers which can extend the set of potentially impacted regions with generated
 * regions by a processor that do not exist in source code. (e.g. <a
 * href="https://projectlombok.org">Lombok</a>)
 */
public interface GeneratedRegionTracker {

  /**
   * Extents set of received potentially impacted regions with generated regions that can be
   * impacted as well.
   *
   * @param regions Set of potentially impacted regions.
   * @return Set of potentially impacted regions including all known potentially impacted generated
   *     regions by this tracker.
   */
  Set<Region> extendWithGeneratedRegions(Set<Region> regions);
}
