package edu.ucr.cs.riple.core.explorers;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.AnnotationInjector;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.metadata.field.FieldDeclarationAnalysis;
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
  private final MethodInheritanceTree methodInheritanceTree;

  public OptimizedExplorer(
      AnnotationInjector injector,
      Bank<Error> errorBank,
      Bank<Fix> fixBank,
      RegionTracker tracker,
      MethodInheritanceTree methodInheritanceTree,
      ImmutableSet<Fix> fixes,
      FieldDeclarationAnalysis fieldDeclarationAnalysis,
      Config config) {
    super(injector, errorBank, fixBank, fixes, fieldDeclarationAnalysis, config);
    this.tracker = tracker;
    this.methodInheritanceTree = methodInheritanceTree;
  }

  @Override
  protected void initializeFixGraph() {
    super.initializeFixGraph();
    this.fixGraph.getAllNodes().forEach(node -> node.updateRegions(tracker));
  }

  @Override
  protected void executeNextCycle() {
    fixGraph.findGroups();
    HashMap<Integer, Set<Node>> groups = fixGraph.getGroups();

    System.out.println(
        "Scheduling for: "
            + groups.size()
            + " builds for: "
            + fixGraph.getAllNodes().size()
            + " fixes");
    ProgressBar pb = Utility.createProgressBar("Processing", groups.size());
    for (Set<Node> group : groups.values()) {
      pb.step();
      Set<Fix> fixes =
          group.stream().flatMap(node -> node.tree.stream()).collect(Collectors.toSet());
      injector.inject(fixes);
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
            node.updateStatus(
                totalEffect,
                fixes,
                localTriggered,
                methodInheritanceTree,
                fieldDeclarationAnalysis);
          });
      injector.remove(fixes);
    }
    pb.close();
  }
}
