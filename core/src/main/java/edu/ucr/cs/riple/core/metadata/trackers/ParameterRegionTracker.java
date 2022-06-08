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

package edu.ucr.cs.riple.core.metadata.trackers;

import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.method.MethodInheritanceTree;
import edu.ucr.cs.riple.injector.location.OnParameter;
import java.util.Set;
import java.util.stream.Collectors;

public class ParameterRegionTracker implements RegionTracker {

  private final MethodInheritanceTree tree;
  private final MethodRegionTracker methodRegionTracker;

  public ParameterRegionTracker(
      MethodInheritanceTree tree, MethodRegionTracker methodRegionTracker) {
    this.tree = tree;
    this.methodRegionTracker = methodRegionTracker;
  }

  @Override
  public Set<Region> getRegions(Fix fix) {
    if (!fix.isOnParameter()) {
      return null;
    }
    OnParameter parameter = fix.toParameter();
    Set<Region> regions =
        tree.getSubMethods(parameter.method, parameter.clazz, false)
            .stream()
            .map(node -> new Region(node.method, node.clazz))
            .collect(Collectors.toSet());
    regions.add(new Region(parameter.method, parameter.clazz));
    regions.addAll(methodRegionTracker.getCallersOfMethod(parameter.clazz, parameter.method));
    return regions;
  }
}
