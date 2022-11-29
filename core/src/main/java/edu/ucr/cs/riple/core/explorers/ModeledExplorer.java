package edu.ucr.cs.riple.core.explorers;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.explorers.suppliers.Supplier;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.models.Model;

public abstract class ModeledExplorer extends Explorer {

  protected Model model;

  public ModeledExplorer(ImmutableSet<Fix> fixes, Supplier supplier) {
    super(fixes, supplier);
  }

  @Override
  public void initializeFixGraph() {
    super.initializeFixGraph();
    this.reports.stream()
        .filter(input -> input.isInProgress(config))
        .flatMap(report -> report.tree.stream().filter(fix -> !model.contains(fix)))
        .forEach(graph::addNodeToVertices);
  }

  @Override
  public void collectGraphResults() {
    graph.getNodes().forEach(model::add);
    // TODO: update reports here.
    throw new RuntimeException("Incomplete feature.");
  }
}
