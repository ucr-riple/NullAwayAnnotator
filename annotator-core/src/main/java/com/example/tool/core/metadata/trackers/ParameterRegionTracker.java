/*
 * MIT License
 *
 * Copyright (c) 2020 anonymous
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

package com.example.tool.core.metadata.trackers;

import com.example.tool.core.metadata.method.MethodDeclarationTree;
import com.example.tool.injector.location.Location;
import com.example.tool.injector.location.OnParameter;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Tracker for Method Parameters. */
public class ParameterRegionTracker implements RegionTracker {

  /**
   * {@link MethodDeclarationTree} instance, used to retrieve regions that will be affected due to
   * inheritance violations.
   */
  private final MethodDeclarationTree tree;

  /** {@link MethodRegionTracker} instance, used to retrieve all sites. */
  private final MethodRegionTracker methodRegionTracker;

  public ParameterRegionTracker(
      MethodDeclarationTree tree, MethodRegionTracker methodRegionTracker) {
    this.tree = tree;
    this.methodRegionTracker = methodRegionTracker;
  }

  @Override
  public Optional<Set<Region>> getRegions(Location location) {
    if (!location.isOnParameter()) {
      return Optional.empty();
    }
    OnParameter parameter = location.toParameter();
    // Get regions which will be potentially affected by inheritance violations.
    Set<Region> regions =
        tree.getSubMethods(parameter.method, parameter.clazz, false).stream()
            .map(node -> new Region(node.location.clazz, node.location.method))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    // Add the method the fix is targeting.
    regions.add(new Region(parameter.clazz, parameter.method));
    // Add all call sites. It will also reserve call sites to prevent callees have their parameters
    // annotated simultaneously. See test: CoreTest#nestedParameters.
    regions.addAll(methodRegionTracker.getCallersOfMethod(parameter.clazz, parameter.method));
    return Optional.of(regions);
  }
}
