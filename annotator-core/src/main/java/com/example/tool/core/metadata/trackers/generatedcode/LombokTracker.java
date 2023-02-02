/*
 * MIT License
 *
 * Copyright (c) 2022 anonymous
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

package com.example.tool.core.metadata.trackers.generatedcode;

import com.example.tool.core.evaluators.graphprocessor.ParallelConflictGraphProcessor;
import com.example.tool.core.metadata.trackers.MethodRegionTracker;
import com.example.tool.core.metadata.trackers.Region;
import com.example.tool.core.metadata.method.MethodDeclarationTree;
import com.example.too.scanner.generatedcode.SourceType;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Tracker for extending potentially impacted regions for elements which will be use in generated
 * code by <a href="https://projectlombok.org">Lombok</a>. Lombok automatically propagates
 * {@code @Nullable} annotation on fields to getter methods, therefore, extends the set of
 * potentially impacted regions to all callers of that method as well. This tracker, will include
 * all callers of any method region in lombok generated code. This will guarantee that {@link
 * ParallelConflictGraphProcessor} will catch any
 * triggered errors by an annotation including all copied annotations by lombok as well.
 */
public class LombokTracker implements GeneratedRegionTracker {

  /** Method region tracker to get potentially impacted regions of a method. */
  private final MethodRegionTracker tracker;
  /** Method declaration tree instance. */
  private final MethodDeclarationTree methodDeclarationTree;

  public LombokTracker(
      MethodDeclarationTree methodDeclarationTree, MethodRegionTracker methodRegionTracker) {
    this.methodDeclarationTree = methodDeclarationTree;
    this.tracker = methodRegionTracker;
  }

  @Override
  public Set<Region> extendForGeneratedRegions(Set<Region> regions) {
    return regions.stream()
        // filter regions which are created by lombok
        .filter(region -> region.sourceType.equals(SourceType.LOMBOK) && region.isOnMethod())
        // find the corresponding method for the region.
        .map(region -> methodDeclarationTree.findNode(region.member, region.clazz))
        .filter(Objects::nonNull)
        // get method location.
        .map(methodNode -> methodNode.location)
        // add potentially impacted regions for the collected methods.
        .flatMap(
            onMethod -> {
              Optional<Set<Region>> ans = tracker.getRegions(onMethod);
              return ans.isPresent() ? ans.get().stream() : Stream.of();
            })
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }
}