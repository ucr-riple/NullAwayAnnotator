package edu.ucr.cs.riple.core.explorers.impactanalyzers;

import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.explorers.suppliers.Supplier;
import edu.ucr.cs.riple.core.metadata.graph.ConflictGraph;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.index.Result;
import edu.ucr.cs.riple.core.util.Utility;
import java.util.Collection;
import java.util.Set;
import me.tongfei.progressbar.ProgressBar;

public class BasicImpactAnalyzer extends ImpactAnalyzer {

  public BasicImpactAnalyzer(Config config, Supplier supplier) {
    super(config, supplier);
  }

  @Override
  public void analyzeImpacts(ConflictGraph graph) {
    System.out.println("Scheduling for: ");
    ProgressBar pb = Utility.createProgressBar("Processing", 0);
    graph
        .getNodes()
        .forEach(
            node -> {
              pb.step();
              Set<Fix> fixes = node.tree;
              injector.injectFixes(fixes);
              compilerRunner.run();
              errorBank.saveState(false, true);
              fixBank.saveState(false, true);
              Result<Error> errorComparisonResult = errorBank.compare();
              node.effect = errorComparisonResult.size;
              Collection<Fix> fixComparisonResultDif = fixBank.compare().dif;
              addTriggeredFixesFromDownstream(node, fixComparisonResultDif);
              node.updateStatus(
                  errorComparisonResult.size,
                  fixes,
                  fixComparisonResultDif,
                  errorComparisonResult.dif,
                  methodDeclarationTree);
              injector.removeFixes(fixes);
            });
    pb.close();
  }
}
