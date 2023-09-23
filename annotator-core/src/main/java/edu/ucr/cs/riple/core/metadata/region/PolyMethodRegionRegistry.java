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
import edu.ucr.cs.riple.injector.location.OnPolyMethod;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class PolyMethodRegionRegistry implements RegionRegistry {

  private final MethodRegionRegistry methodRegionRegistry;

  public PolyMethodRegionRegistry(MethodRegionRegistry methodRegionRegistry) {
    this.methodRegionRegistry = methodRegionRegistry;
  }

  @Override
  public Optional<Set<Region>> getImpactedRegions(Location location) {
    if (!location.isOnPolyMethod()) {
      return Optional.empty();
    }
    Set<Region> regions = new HashSet<>();
    OnPolyMethod onPolyMethod = location.toPolyMethod();
    // On Method itself:
    Region onMethod = new Region(onPolyMethod.clazz, onPolyMethod.method);
    regions.add(onMethod);
    regions.addAll(
        methodRegionRegistry.getCallersOfMethod(onPolyMethod.clazz, onPolyMethod.method));
    return Optional.of(regions);
  }
}
