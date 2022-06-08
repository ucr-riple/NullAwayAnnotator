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

public abstract class Explorer {
  protected final AnnotationInjector injector;
  protected final Bank<Error> errorBank;
  protected final Bank<Fix> fixBank;
  protected final ImmutableSet<Report> reports;
  protected final Config config;
  protected final FixGraph<Node> fixGraph;

  public Explorer(
      AnnotationInjector injector,
      Bank<Error> errorBank,
      Bank<Fix> fixBank,
      ImmutableSet<Fix> fixes,
      Config config) {
    this.injector = injector;
    this.errorBank = errorBank;
    this.fixBank = fixBank;
    this.reports =
        fixes.stream().map(fix -> new Report(fix, -1)).collect(ImmutableSet.toImmutableSet());
    this.config = config;
    this.fixGraph = new FixGraph<>(Node::new);
  }

  protected void initializeFixGraph() {
    this.fixGraph.clear();
    this.reports
        .stream()
        .filter(
            report ->
                ((!config.bailout || report.effect > 0) && !report.finished) || !report.processed)
        .forEach(
            report -> {
              Fix root = report.root;
              Node node = fixGraph.findOrCreate(root);
              node.setRootSource(fixBank);
              node.report = report;
              node.triggered = report.triggered;
              node.tree.addAll(report.tree);
              node.mergeTriggered();
              node.changed = false;
            });
  }

  protected void finalizeReports() {
    fixGraph
        .getAllNodes()
        .forEach(
            node -> {
              Report report = node.report;
              report.effect = node.effect;
              report.tree = node.tree;
              report.triggered = node.triggered;
              report.finished = !node.changed;
              report.processed = true;
            });
  }

  protected abstract void executeNextCycle();

  public ImmutableSet<Report> explore() {
    System.out.println("Max Depth level: " + config.depth);
    for (int i = 0; i < config.depth; i++) {
      System.out.print("Analyzing at level " + (i + 1) + ", ");
      initializeFixGraph();
      config.log.updateNodeNumber(fixGraph.getAllNodes().size());
      if (fixGraph.isEmpty()) {
        System.out.println("Analysis finished at this iteration.");
        break;
      }
      executeNextCycle();
      finalizeReports();
    }
    return reports;
  }
}
