package edu.ucr.cs.riple.core.explorers;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.AnnotationInjector;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.metadata.graph.FixGraph;
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

  private final FixGraph<Node> fixGraph;
  private final RegionTracker tracker;
  private final MethodInheritanceTree methodInheritanceTree;

  public OptimizedExplorer(
      AnnotationInjector injector,
      Bank<Error> errorBank,
      Bank<Fix> fixBank,
      RegionTracker tracker,
      MethodInheritanceTree methodInheritanceTree,
      ImmutableSet<Fix> fixes,
      Config config) {
    super(injector, errorBank, fixBank, fixes, config);
    this.tracker = tracker;
    this.methodInheritanceTree = methodInheritanceTree;
    this.fixGraph = new FixGraph<>(Node::new);
  }

  @Override
  protected boolean initializeReports() {
    this.fixGraph.clear();
    return this.reports
            .stream()
            .filter(
                report ->
                    ((!config.bailout || report.effect > 0) && !report.finished)
                        || !report.processed)
            .peek(
                report -> {
                  Fix root = report.root;
                  Node node = fixGraph.findOrCreate(root);
                  node.setRootSource(fixBank);
                  node.report = report;
                  node.triggered = report.triggered;
                  node.tree.addAll(report.tree);
                  node.mergeTriggered();
                  node.updateRegions(tracker);
                  node.changed = false;
                })
            .collect(Collectors.toSet())
            .size()
        > 0;
  }

  @Override
  protected void executeNextCycle() {
    if (fixGraph.nodes.size() == 0) {
      return;
    }
    fixGraph.findGroups();
    HashMap<Integer, Set<Node>> groups = fixGraph.getGroups();
    System.out.println(
        "Scheduling for: "
            + groups.size()
            + " builds for: "
            + fixGraph.getAllNodes().size()
            + " reports");
    ProgressBar pb = Utility.createProgressBar("Analysing", groups.size());
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
            node.updateStatus(totalEffect, fixes, localTriggered, methodInheritanceTree);
          });
      injector.remove(fixes);
    }
    pb.close();
  }

  @Override
  protected void finalizeReports() {
    List<Node> nodes = fixGraph.getAllNodes();
    nodes.forEach(
        node -> {
          Report report = node.report;
          report.effect = node.effect;
          report.tree = node.tree;
          report.triggered = node.triggered;
          report.finished = !node.changed;
          report.processed = true;
        });
  }
}
