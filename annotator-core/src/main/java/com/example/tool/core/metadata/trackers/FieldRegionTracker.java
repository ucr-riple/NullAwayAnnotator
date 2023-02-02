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

import com.example.tool.core.ModuleInfo;
import com.example.tool.core.metadata.MetaData;
import com.example.tool.core.Config;
import com.example.tool.core.metadata.method.MethodDeclarationTree;
import com.example.tool.injector.location.Location;
import com.example.tool.injector.location.OnField;
import com.example.too.scanner.Serializer;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Tracker for Fields. */
public class FieldRegionTracker extends MetaData<TrackerNode> implements RegionTracker {

  private final MethodDeclarationTree tree;

  public FieldRegionTracker(Config config, ModuleInfo info, MethodDeclarationTree tree) {
    super(config, info.dir.resolve(Serializer.FIELD_GRAPH_FILE_NAME));
    this.tree = tree;
  }

  @Override
  protected TrackerNode addNodeByLine(String[] values) {
    return config.getAdapter().deserializeTrackerNode(values);
  }

  @Override
  public Optional<Set<Region>> getRegions(Location location) {
    if (!location.isOnField()) {
      return Optional.empty();
    }
    OnField field = location.toField();
    // Add all regions where the field is assigned a new value or read.
    Set<Region> ans =
        findNodesWithHashHint(
                candidate ->
                    candidate.calleeClass.equals(field.clazz)
                        && field.isOnFieldWithName(candidate.calleeMember),
                TrackerNode.hash(field.clazz))
            .map(trackerNode -> trackerNode.region)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    ans.addAll(config.getAdapter().getFieldRegionScope(field));
    ans.addAll(
        tree.getConstructorsForClass(field.clazz).stream()
            .map(methodNode -> new Region(methodNode.location.clazz, methodNode.location.method))
            .collect(Collectors.toSet()));
    return Optional.of(ans);
  }
}