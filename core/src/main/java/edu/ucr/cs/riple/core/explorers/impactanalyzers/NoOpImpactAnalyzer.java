package edu.ucr.cs.riple.core.explorers.impactanalyzers;

import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.explorers.suppliers.Supplier;
import edu.ucr.cs.riple.core.metadata.graph.ConflictGraph;

public class NoOpImpactAnalyzer extends ImpactAnalyzer {

  public NoOpImpactAnalyzer(Config config, Supplier supplier) {
    super(config, supplier);
  }

  @Override
  public void analyzeImpacts(ConflictGraph graph) {
    // no op
  }
}
