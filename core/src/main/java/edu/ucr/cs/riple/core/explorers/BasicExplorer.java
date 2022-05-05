package edu.ucr.cs.riple.core.explorers;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.AnnotationInjector;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.metadata.graph.Node;
import edu.ucr.cs.riple.core.metadata.index.Bank;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.index.Result;
import edu.ucr.cs.riple.core.util.Utility;
import java.util.Set;
import me.tongfei.progressbar.ProgressBar;

public class BasicExplorer extends Explorer {

  public BasicExplorer(
      AnnotationInjector injector,
      Bank<Error> errorBank,
      Bank<Fix> fixBank,
      ImmutableSet<Fix> fixes,
      Config config) {
    super(injector, errorBank, fixBank, fixes, config);
  }

  @Override
  protected void executeNextCycle() {
    System.out.println(
        "Scheduling for: " + reports.size() + " builds for: " + reports.size() + " fixes");
    ProgressBar pb = Utility.createProgressBar("Analysing", reports.size());
    for (Node node : fixGraph.getAllNodes()) {
      pb.step();
      Set<Fix> fixes = node.tree;
      injector.inject(fixes);
      Utility.buildProject(config);
      errorBank.saveState(false, true);
      fixBank.saveState(false, true);
      int totalEffect = 0;
      Result<Error> res = errorBank.compare();
      totalEffect += res.size;
      node.effect = totalEffect;
      node.updateTriggered(fixBank.compare().dif);
      injector.remove(fixes);
    }
    pb.close();
  }
}
