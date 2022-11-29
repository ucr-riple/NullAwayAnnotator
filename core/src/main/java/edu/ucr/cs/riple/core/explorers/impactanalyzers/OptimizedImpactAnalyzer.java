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

public class OptimizedImpactAnalyzer extends ImpactAnalyzer {

  private final RegionTracker regionTracker;

  public OptimizedImpactAnalyzer(Config config, Supplier supplier, RegionTracker regionTracker) {
    super(config, supplier);
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
