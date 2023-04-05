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
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.Context;
import edu.ucr.cs.riple.core.ModuleInfo;
import edu.ucr.cs.riple.core.metadata.trackers.generatedcode.GeneratedRegionTracker;
import edu.ucr.cs.riple.core.metadata.trackers.generatedcode.LombokTracker;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.scanner.generatedcode.SourceType;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/** Container class for all trackers. This tracker can handle all fix types. */
public class CompoundTracker implements RegionTracker {

  /** List of all trackers. */
  private final ImmutableSet<RegionTracker> trackers;

  private final ImmutableSet<GeneratedRegionTracker> generatedRegionsTrackers;

  public CompoundTracker(Config config, ModuleInfo info, Context context) {
    MethodRegionTracker methodRegionTracker = new MethodRegionTracker(config, info, context);
    this.trackers =
        ImmutableSet.of(
            new FieldRegionTracker(config, info, context),
            methodRegionTracker,
            new ParameterRegionTracker(context, methodRegionTracker));
    ImmutableSet.Builder<GeneratedRegionTracker> generatedRegionTrackerBuilder =
        new ImmutableSet.Builder<>();
    if (config.generatedCodeDetectors.contains(SourceType.LOMBOK)) {
      generatedRegionTrackerBuilder.add(new LombokTracker(context, methodRegionTracker));
    }
    this.generatedRegionsTrackers = generatedRegionTrackerBuilder.build();
  }

  @Override
  public Optional<Set<Region>> getRegions(Location location) {
    Set<Region> regions = new HashSet<>();
    this.trackers.forEach(tracker -> tracker.getRegions(location).ifPresent(regions::addAll));
    this.generatedRegionsTrackers.forEach(
        tracker -> regions.addAll(tracker.extendForGeneratedRegions(regions)));
    return Optional.of(regions);
  }
}
