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

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.ModuleInfo;
import edu.ucr.cs.riple.core.adapters.Adapter;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.method.MethodDeclarationTree;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Container class for all trackers. This tracker can handle all fix types. */
public class CompoundTracker implements RegionTracker {

  /** List of all trackers. */
  private final List<RegionTracker> trackers;

  public CompoundTracker(Adapter adapter, ModuleInfo info, MethodDeclarationTree tree) {
    this.trackers = new ArrayList<>();
    MethodRegionTracker methodRegionTracker = new MethodRegionTracker(adapter, info, tree);
    this.trackers.add(new FieldRegionTracker(adapter, info));
    this.trackers.add(methodRegionTracker);
    this.trackers.add(new ParameterRegionTracker(tree, methodRegionTracker));
  }

  public CompoundTracker(
      Adapter adapter, ImmutableSet<ModuleInfo> modules, MethodDeclarationTree tree) {
    this.trackers = new ArrayList<>();
    MethodRegionTracker methodRegionTracker = new MethodRegionTracker(adapter, modules, tree);
    this.trackers.add(new FieldRegionTracker(adapter, modules));
    this.trackers.add(methodRegionTracker);
    this.trackers.add(new ParameterRegionTracker(tree, methodRegionTracker));
  }

  @Override
  public Optional<Set<Region>> getRegions(Fix location) {
    for (RegionTracker tracker : this.trackers) {
      Optional<Set<Region>> ans = tracker.getRegions(location);
      if (ans.isPresent()) {
        return ans;
      }
    }
    throw new IllegalStateException("Region cannot be null at this point." + location);
  }
}
