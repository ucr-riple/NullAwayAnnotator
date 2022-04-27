package edu.ucr.cs.riple.core.explorers;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.AnnotationInjector;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.metadata.index.Bank;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.index.Result;
import edu.ucr.cs.riple.core.util.Utility;
import java.util.Collections;
import java.util.HashSet;
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
  protected boolean initializeReports() {
    return true;
  }

  @Override
  protected void finalizeReports() {}

  @Override
  protected void executeNextCycle() {
    System.out.println(
        "Scheduling for: "
            + this.reports.size()
            + " builds for: "
            + this.reports.size()
            + " reports");
    ProgressBar pb = Utility.createProgressBar("Analysing", this.reports.size());
    for (Report report : this.reports) {
      pb.step();
      injector.inject(Collections.singleton(report.root));
      Utility.buildProject(config);
      errorBank.saveState(false, true);
      fixBank.saveState(false, true);
      Result<Error> res = errorBank.compare();
      report.effect = res.size;
      report.triggered = new HashSet<>(fixBank.compare().dif);
      injector.remove(Collections.singleton(report.root));
    }
    pb.close();
  }
}
