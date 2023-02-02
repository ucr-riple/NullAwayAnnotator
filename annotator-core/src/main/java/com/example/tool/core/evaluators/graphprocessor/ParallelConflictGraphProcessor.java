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

package com.example.tool.core.evaluators.graphprocessor;

import com.example.tool.core.Config;
import com.example.tool.core.evaluators.suppliers.Supplier;
import com.example.tool.core.metadata.graph.ConflictGraph;
import com.example.tool.core.metadata.graph.Node;
import com.example.tool.core.metadata.index.Error;
import com.example.tool.core.metadata.index.Fix;
import com.example.tool.core.metadata.index.Result;
import com.example.tool.core.metadata.trackers.Region;
import com.example.tool.core.metadata.trackers.RegionTracker;
import com.example.tool.core.util.Utility;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import me.tongfei.progressbar.ProgressBar;

/**
 * Parallel processor which computes the impact of nodes in parallel. In this processor,
 * non-conflicting nodes are processed simultaneously. The conflict graph will be used to determine
 * the minimum number of non-conflicting groups using graph coloring techniques.
 */
public class ParallelConflictGraphProcessor extends AbstractConflictGraphProcessor {

  /** Tracker instance to check conflicts. */
  private final RegionTracker regionTracker;

  public ParallelConflictGraphProcessor(
      Config config, CompilerRunner runner, Supplier supplier, RegionTracker regionTracker) {
    super(config, runner, supplier);
    this.regionTracker = regionTracker;
  }

  @Override
  public void process(ConflictGraph graph) {
    graph.getNodes().forEach(node -> node.reCollectPotentiallyImpactedRegions(regionTracker));
    // find non-conflicting groups.
    graph.findGroups();
    Collection<Set<Node>> nonConflictingGroups = graph.getGroups();
    System.out.println(
        "Scheduling for: "
            + nonConflictingGroups.size()
            + " builds for: "
            + graph.getNodes().count()
            + " fixes");
    ProgressBar pb = Utility.createProgressBar("Processing", nonConflictingGroups.size());
    for (Set<Node> group : nonConflictingGroups) {
      pb.step();
      Set<Fix> fixes =
          group.stream()
              .flatMap(node -> node.tree.stream())
              .collect(Collectors.toCollection(LinkedHashSet::new));
      injector.injectFixes(fixes);
      compilerRunner.run();
      errorStore.saveState();
      group.forEach(
          node -> {
            int localEffect = 0;
            Set<Error> triggeredErrors = new LinkedHashSet<>();
            for (Region region : node.regions) {
              Result errorComparisonResult = errorStore.compareByRegion(region);
              localEffect += errorComparisonResult.size;
              triggeredErrors.addAll(errorComparisonResult.dif);
            }
            node.updateStatus(
                localEffect,
                fixes,
                getTriggeredFixesFromDownstream(node),
                triggeredErrors,
                methodDeclarationTree);
          });
      injector.removeFixes(fixes);
    }
    pb.close();
  }
}