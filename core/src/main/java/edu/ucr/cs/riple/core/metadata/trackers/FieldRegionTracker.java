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

import edu.ucr.cs.riple.core.FixType;
import edu.ucr.cs.riple.core.metadata.MetaData;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

public class FieldRegionTracker extends MetaData<TrackerNode> implements RegionTracker {

  private final FixType fixType;

  public FieldRegionTracker(Path path) {
    super(path);
    this.fixType = FixType.FIELD;
  }

  @Override
  protected TrackerNode addNodeByLine(String[] values) {
    return new TrackerNode(values[0], values[1], values[2], values[3]);
  }

  @Override
  public Set<Region> getRegions(Fix fix) {
    if (!fix.kind.equals(fixType.name)) {
      return null;
    }
    return findAllNodes(
            candidate ->
                candidate.calleeClass.equals(fix.clazz)
                    && candidate.calleeMember.equals(fix.variable),
            fix.variable,
            fix.clazz)
        .stream()
        .map(node -> new Region(node.callerMethod, node.callerClass))
        .collect(Collectors.toSet());
  }
}
