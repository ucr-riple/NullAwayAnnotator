/*
 * MIT License
 *
 * Copyright (c) 2022 Nima Karimipour
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

package edu.ucr.cs.riple.core.explorers.impactanalyzers;

import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.explorers.suppliers.Supplier;
import edu.ucr.cs.riple.core.metadata.graph.ConflictGraph;
import edu.ucr.cs.riple.core.metadata.graph.Node;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.index.Result;
import edu.ucr.cs.riple.core.metadata.trackers.Region;
import edu.ucr.cs.riple.core.metadata.trackers.RegionTracker;
import edu.ucr.cs.riple.core.util.Utility;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import me.tongfei.progressbar.ProgressBar;

public class OptimizedImpactAnalyzer extends AbstractImpactAnalyzer {

  private final RegionTracker regionTracker;

  public OptimizedImpactAnalyzer(
      Config config, CompilerRunner runner, Supplier supplier, RegionTracker regionTracker) {
    super(config, runner, supplier);
    this.regionTracker = regionTracker;
  }

  @Override
  public void analyzeImpacts(ConflictGraph graph) {
    graph.getNodes().forEach(node -> node.reCollectPotentiallyImpactedRegions(regionTracker));
    graph.findGroups();
    Collection<Set<Node>> groups = graph.getGroups();
    System.out.println(
        "Scheduling for: " + groups.size() + " builds for: " + graph.getNodes().count() + " fixes");
    ProgressBar pb = Utility.createProgressBar("Processing", groups.size());
    for (Set<Node> group : groups) {
      pb.step();
      Set<Fix> fixes =
          group.stream().flatMap(node -> node.tree.stream()).collect(Collectors.toSet());
      injector.injectFixes(fixes);
      compilerRunner.run();
      errorBank.saveState(false, true);
      fixBank.saveState(false, true);
      group.forEach(
          node -> {
            int localEffect = 0;
            List<Fix> triggeredFixes = new ArrayList<>();
            List<Error> triggeredErrors = new ArrayList<>();
            for (Region region : node.regions) {
              Result<Error> errorComparisonResult =
                  errorBank.compareByMember(region.clazz, region.member, false);
              localEffect += errorComparisonResult.size;
              triggeredErrors.addAll(errorComparisonResult.dif);
              triggeredFixes.addAll(
                  fixBank.compareByMember(region.clazz, region.member, false).dif);
            }
            addTriggeredFixesFromDownstream(node, triggeredFixes);
            node.updateStatus(
                localEffect, fixes, triggeredFixes, triggeredErrors, methodDeclarationTree);
          });
      injector.removeFixes(fixes);
    }
    pb.close();
  }
}
