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

package edu.ucr.cs.riple.core.evaluators.graphprocessor;

import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.evaluators.suppliers.Supplier;
import edu.ucr.cs.riple.core.metadata.graph.ConflictGraph;
import edu.ucr.cs.riple.core.metadata.graph.Node;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.index.Result;
import edu.ucr.cs.riple.core.metadata.trackers.Region;
import edu.ucr.cs.riple.core.metadata.trackers.RegionTracker;
import edu.ucr.cs.riple.core.util.Utility;
import java.util.Collection;
import java.util.HashSet;
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
    ProgressBar pb = Utility.createProgressBar("Processing", nonConflictingGroups.size());
    for (Set<Node> group : nonConflictingGroups) {
      pb.step();
      Set<Fix> fixes =
          group.stream().flatMap(node -> node.tree.stream()).collect(Collectors.toSet());
      injector.injectFixes(fixes);
      compilerRunner.run();
      errorStore.saveState();
      group.forEach(
          node -> {
            int localEffect = 0;
            Set<Error> triggeredErrors = new HashSet<>();
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
