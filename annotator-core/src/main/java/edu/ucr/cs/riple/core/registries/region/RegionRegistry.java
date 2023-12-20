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
import edu.ucr.cs.riple.core.registries.index.Fix;
import edu.ucr.cs.riple.injector.location.Location;

/**
 * Interface for region registries. Region registries can locate regions where a {@link Fix} can
 * potentially introduce new errors if injected.
 */
public interface RegionRegistry {

  /**
   * Returns Set of regions where a fix can introduce new errors if injected.
   *
   * @param location Location targeted by the fix.
   * @return Immutable Set of regions.
   */
  ImmutableSet<Region> getImpactedRegions(Location location);

  /**
   * Returns the set of regions where the element targeted by the passed location has been used.
   * For:
   *
   * <ul>
   *   <li>methods, it returns the set of regions where the method has been called.
   *   <li>fields, it returns the set of regions where the field has been accessed.
   *   <li>parameters, it returns the enclosing method.
   * </ul>
   *
   * @param location Location targeted by the fix.
   * @return Immutable Set of regions where the passed location's targeted element has been used.
   */
  ImmutableSet<Region> getImpactedRegionsByUse(Location location);
}
