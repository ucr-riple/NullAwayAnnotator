package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.Diagnose;
import edu.ucr.cs.riple.autofixer.DiagnoseReport;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.autofixer.nullaway.AutoFixConfig;
import edu.ucr.cs.riple.autofixer.nullaway.Writer;
import edu.ucr.cs.riple.injector.Fix;
import java.io.File;

public abstract class Explorer {

  protected final Diagnose diagnose;
  protected final Bank bank;

  public Explorer(Diagnose diagnose, Bank bank) {
    this.diagnose = diagnose;
    this.bank = bank;
  }

  public DiagnoseReport effect(Fix fix) {
    diagnose.buildProject();
    File tempFile = new File(Writer.ERROR);
    boolean exists = tempFile.exists();
    if (exists) {
      return new DiagnoseReport(fix, bank.compare());
    }
    return DiagnoseReport.empty(fix);
  }

  public DiagnoseReport effectByScope(Fix fix, String[] workList) {
    if (workList == null) {
      return new DiagnoseReport(fix, -1);
    }
    AutoFixConfig.AutoFixConfigWriter writer =
        new AutoFixConfig.AutoFixConfigWriter()
            .setLogError(true, false)
            .setMakeCallGraph(false)
            .setMakeFieldGraph(false)
            .setOptimized(false)
            .setMethodInheritanceTree(false)
            .setSuggest(true)
            .setWorkList(workList);
    diagnose.writeConfig(writer);
    diagnose.buildProject();
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
