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
import edu.ucr.cs.riple.core.metadata.MetaData;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.method.MethodInheritanceTree;
import edu.ucr.cs.riple.core.metadata.method.MethodNode;
import edu.ucr.cs.riple.injector.location.OnMethod;
import edu.ucr.cs.riple.scanner.Serializer;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Tracker for Methods. */
public class MethodRegionTracker extends MetaData<TrackerNode> implements RegionTracker {

  /**
   * {@link MethodInheritanceTree} instance, used to retrieve regions that will be affected due to
   * inheritance violations.
   */
  private final MethodInheritanceTree tree;

  public MethodRegionTracker(ModuleInfo info, MethodInheritanceTree tree) {
    super(info.dir.resolve(Serializer.CALL_GRAPH_FILE_NAME));
    this.tree = tree;
  }

  public MethodRegionTracker(Stream<ModuleInfo> modules, MethodInheritanceTree tree) {
    super(
        modules
            .map(info -> info.dir.resolve(Serializer.CALL_GRAPH_FILE_NAME))
            .collect(ImmutableSet.toImmutableSet()));
    this.tree = tree;
  }

  @Override
  protected TrackerNode addNodeByLine(String[] values) {
    return new TrackerNode(values[0], values[1], values[2], values[3]);
  }

  @Override
  public Optional<Set<Region>> getRegions(Fix fix) {
    if (!fix.isOnMethod()) {
      return Optional.empty();
    }
    OnMethod onMethod = fix.toMethod();
    // Add callers of method.
    Set<Region> regions = getCallersOfMethod(onMethod.clazz, onMethod.method);
    // Add immediate super method.
    MethodNode parent = tree.getClosestSuperMethod(onMethod.method, onMethod.clazz);
    if (parent != null) {
      regions.add(new Region(parent.clazz, parent.method));
    }
    return Optional.of(regions);
  }

  /**
   * Returns set of regions where the target method is called.
   *
   * @param clazz Fully qualified name of the class of the target method.
   * @param method Method signature.
   * @return Set of regions where target method is called.
   */
  public Set<Region> getCallersOfMethod(String clazz, String method) {
    return findNodesWithHashHint(
            candidate ->
                candidate.calleeClass.equals(clazz) && candidate.calleeMember.equals(method),
            TrackerNode.hash(clazz))
        .map(node -> new Region(node.callerClass, node.callerMethod))
        .collect(Collectors.toSet());
  }
}
