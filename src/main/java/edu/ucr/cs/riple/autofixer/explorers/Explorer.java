package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.Diagnose;
import edu.ucr.cs.riple.autofixer.DiagnoseReport;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.autofixer.nullaway.AutoFixConfig;
import edu.ucr.cs.riple.autofixer.nullaway.Writer;
import edu.ucr.cs.riple.injector.Fix;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class Explorer {

  protected final Diagnose diagnose;
  protected final Bank bank;

  public Explorer(Diagnose diagnose, Bank bank) {
    this.diagnose = diagnose;
    this.bank = bank;
  }

  public DiagnoseReport effect(Fix fix) {
    AutoFixConfig.AutoFixConfigWriter config =
        new AutoFixConfig.AutoFixConfigWriter()
            .setLogError(true, false)
            .setMakeCallGraph(false)
            .setMakeFieldGraph(false)
            .setOptimized(false)
            .setMethodInheritanceTree(false)
            .setSuggest(true)
            .setWorkList(new String[] {"*"});
    diagnose.buildProject(config);
    if (new File(Writer.ERROR).exists()) {
      return new DiagnoseReport(fix, bank.compare());
    }
    return DiagnoseReport.empty(fix);
  }

  public DiagnoseReport effectByScope(Fix fix, List<String> workList) {
    if (workList == null) {
      workList = new ArrayList<>();
    }
    workList.add(fix.className);
    AutoFixConfig.AutoFixConfigWriter config =
        new AutoFixConfig.AutoFixConfigWriter()
            .setLogError(true, true)
            .setMakeCallGraph(false)
            .setMakeFieldGraph(false)
            .setOptimized(false)
            .setMethodInheritanceTree(false)
            .setSuggest(true)
            .setWorkList(workList.toArray(new String[0]));
    diagnose.buildProject(config);
    if (new File(Writer.ERROR).exists()) {
      int totalEffect = 0;
      for (String clazz : workList) {
        if (clazz.equals(fix.className)) {
          continue;
        }
        totalEffect += bank.compareByClass(clazz, true);
      }
      totalEffect += bank.compareByClass(fix.className, true);
      return new DiagnoseReport(fix, totalEffect);
    }
    return DiagnoseReport.empty(fix);
  }

  public abstract boolean isApplicable(Fix fix);
}
