package edu.ucr.cs.riple.core.explorers.impactanalyzers;

import edu.ucr.cs.riple.core.metadata.graph.ConflictGraph;

public interface ImpactAnalyzer {

  void analyzeImpacts(ConflictGraph graph);
}
