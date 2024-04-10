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

package edu.ucr.cs.riple.core.evaluators.graph.processors;

import edu.ucr.cs.riple.core.Context;
import edu.ucr.cs.riple.core.evaluators.graph.ConflictGraph;
import edu.ucr.cs.riple.core.evaluators.graph.Node;
import edu.ucr.cs.riple.core.evaluators.suppliers.Supplier;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.index.Result;
import edu.ucr.cs.riple.core.metadata.region.Region;
import edu.ucr.cs.riple.core.metadata.region.RegionRegistry;
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

  /**
   * Region registry instance to check conflicts in potentially impacted regions by changes in fix
   * trees.
   */
  private final RegionRegistry regionRegistry;

  public ParallelConflictGraphProcessor(Context context, CompilerRunner runner, Supplier supplier) {
    super(context, runner, supplier);
    this.regionRegistry = supplier.getModuleInfo().getRegionRegistry();
  }

  @Override
  public void process(ConflictGraph graph) {
    graph.getNodes().forEach(node -> node.reCollectPotentiallyImpactedRegions(regionRegistry));
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
      //      if (group.stream().noneMatch(node -> edu.ucr.cs.riple.core.Main.isTheFix(node.root)))
      // {
      //        continue;
      //      }
      pb.step();
      Set<Fix> fixes =
          group.stream().flatMap(node -> node.tree.stream()).collect(Collectors.toSet());
      injector.injectFixes(fixes);
      Utility.timeStamp(context.config);
      compilerRunner.run();
      errorStore.saveState();
      group.forEach(
          node -> {
            int localEffect = 0;
            Set<Error> triggeredErrors = new HashSet<>();
            for (Region region : node.regions) {
              ;
              Result errorComparisonResult = errorStore.compareByRegion(region);
              localEffect += errorComparisonResult.size;
              triggeredErrors.addAll(errorComparisonResult.dif);
            }
            node.updateStatus(
                localEffect,
                fixes,
                getTriggeredFixesFromDownstreamErrors(node),
                triggeredErrors,
                moduleInfo);
          });
      injector.removeFixes(fixes);
    }
    pb.close();
  }
}
