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

package edu.ucr.cs.riple.core.explorers;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.injectors.AnnotationInjector;
import edu.ucr.cs.riple.core.metadata.graph.Node;
import edu.ucr.cs.riple.core.metadata.index.Bank;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.index.Result;
import edu.ucr.cs.riple.core.metadata.method.MethodInheritanceTree;
import edu.ucr.cs.riple.core.metadata.trackers.Region;
import edu.ucr.cs.riple.core.metadata.trackers.RegionTracker;
import edu.ucr.cs.riple.core.util.Utility;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import me.tongfei.progressbar.ProgressBar;

public class OptimizedExplorer extends Explorer {
  private final RegionTracker tracker;

  public OptimizedExplorer(
      AnnotationInjector injector,
      Bank<Error> errorBank,
      Bank<Fix> fixBank,
      RegionTracker tracker,
      ImmutableSet<Fix> fixes,
      MethodInheritanceTree methodInheritanceTree,
      int depth,
      Config config) {
    super(injector, errorBank, fixBank, fixes, methodInheritanceTree, depth, config);
    this.tracker = tracker;
  }

  @Override
  protected void initializeFixGraph() {
    super.initializeFixGraph();
    this.graph.getAllNodes().forEach(node -> node.reCollectPotentiallyImpactedRegions(tracker));
  }

  @Override
  protected void executeNextCycle() {
    graph.findGroups();
    HashMap<Integer, Set<Node>> groups = graph.getGroups();
    System.out.println(
        "Scheduling for: "
            + groups.size()
            + " builds for: "
            + graph.getAllNodes().size()
            + " fixes");
    ProgressBar pb = Utility.createProgressBar("Processing", groups.size());
    for (Set<Node> group : groups.values()) {
      pb.step();
      Set<Fix> fixes =
          group.stream().flatMap(node -> node.tree.stream()).collect(Collectors.toSet());
      injector.injectFixes(fixes);
      Utility.buildProject(config);
      errorBank.saveState(false, true);
      fixBank.saveState(false, true);
      group.forEach(
          node -> {
            int totalEffect = 0;
            List<Fix> localTriggered = new ArrayList<>();
            for (Region region : node.regions) {
              Result<Error> res = errorBank.compareByMethod(region.clazz, region.method, false);
              totalEffect += res.size;
              localTriggered.addAll(
                  new ArrayList<>(fixBank.compareByMethod(region.clazz, region.method, false).dif));
            }
            node.updateStatus(totalEffect, fixes, localTriggered, methodInheritanceTree);
          });
      injector.removeFixes(fixes);
    }
    pb.close();
  }
}
