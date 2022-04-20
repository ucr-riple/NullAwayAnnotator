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

import edu.ucr.cs.css.Serializer;
import edu.ucr.cs.riple.core.FixType;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CompoundTracker implements RegionTracker {

  private final List<RegionTracker> trackers;

  public CompoundTracker(Path dir) {
    this.trackers = new ArrayList<>();
    this.trackers.add(new FieldRegionTracker(dir.resolve(Serializer.FIELD_GRAPH_NAME)));
    this.trackers.add(new MethodRegionTracker(dir.resolve(Serializer.CALL_GRAPH_NAME)));
    this.trackers.add(
        fix -> {
          if (!fix.kind.equals(FixType.PARAMETER.name)) {
            return null;
          }
          return Collections.singleton(new Region(fix.method, fix.clazz));
        });
  }

  @Override
  public Set<Region> getRegions(Fix location) {
    for (RegionTracker tracker : this.trackers) {
      Set<Region> ans = tracker.getRegions(location);
      if (ans != null) {
        return ans;
      }
    }
    throw new IllegalStateException("Region cannot be null at this point." + location);
  }
}
