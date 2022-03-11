/*
 * Copyright (c) 2022 University of California, Riverside.
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
import edu.ucr.cs.riple.core.metadata.AbstractRelation;
import edu.ucr.cs.riple.injector.Fix;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MethodUsageTracker extends AbstractRelation<TrackerNode> implements UsageTracker {
  private final FixType fixType;

  public MethodUsageTracker(Path path) {
    super(path);
    this.fixType = FixType.METHOD;
  }

  @Override
  protected TrackerNode addNodeByLine(String[] values) {
    return new TrackerNode(values[0], values[1], values[2], values[3]);
  }

  @Override
  public Set<String> getUsers(Fix fix) {
    if (!fix.location.equals(fixType.name)) {
      return null;
    }
    List<TrackerNode> nodes =
        findAllNodes(
            candidate ->
                candidate.calleeClass.equals(fix.className)
                    && candidate.calleeMember.equals(fix.method),
            fix.method,
            fix.className);
    return nodes
        .stream()
        .map(callGraphNode -> callGraphNode.callerClass)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<Usage> getUsage(Fix fix) {
    if (!fix.location.equals(fixType.name)) {
      return null;
    }
    List<TrackerNode> nodes =
        findAllNodes(
            candidate ->
                candidate.calleeClass.equals(fix.className)
                    && candidate.calleeMember.equals(fix.method),
            fix.method,
            fix.className);
    Set<Usage> ans =
        nodes
            .stream()
            .map(callUsageNode -> new Usage(callUsageNode.callerMethod, callUsageNode.callerClass))
            .collect(Collectors.toSet());
    ans.add(new Usage(fix.method, fix.className));
    return ans;
  }
}
