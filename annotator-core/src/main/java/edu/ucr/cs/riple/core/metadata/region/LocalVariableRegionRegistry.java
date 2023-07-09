/*
 * MIT License
 *
 * Copyright (c) 2023 Nima Karimipour
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

package edu.ucr.cs.riple.core.metadata.region;

import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.OnLocalVariable;
import java.util.Optional;
import java.util.Set;

/** Region Registry for Local variables. */
public class LocalVariableRegionRegistry implements RegionRegistry {
  @Override
  public Optional<Set<Region>> getImpactedRegions(Location location) {
    if (!location.isOnLocalVariable()) {
      return Optional.empty();
    }
    OnLocalVariable localVariable = location.toLocalVariable();
    // If null, this location points to a local variable inside a static initializer block.
    String regionMember = localVariable.encMethod == null ? "" : localVariable.encMethod.method;
    return Optional.of(Set.of(new Region(localVariable.clazz, regionMember)));
  }
}
