package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.AutoFixer;
import edu.ucr.cs.riple.autofixer.Report;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.autofixer.nullaway.AutoFixConfig;
import edu.ucr.cs.riple.autofixer.nullaway.Writer;
import edu.ucr.cs.riple.injector.Fix;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class Explorer {

  protected final AutoFixer autoFixer;
  protected final Bank bank;

  public Explorer(AutoFixer autoFixer, Bank bank) {
    this.autoFixer = autoFixer;
    this.bank = bank;
  }

  public Report effect(Fix fix) {
    AutoFixConfig.AutoFixConfigWriter config =
        new AutoFixConfig.AutoFixConfigWriter()
            .setLogError(true, false)
            .setMakeCallGraph(false)
            .setMakeFieldGraph(false)
            .setOptimized(false)
            .setMethodInheritanceTree(false)
            .setSuggest(true)
            .setWorkList(new String[] {"*"});
    autoFixer.buildProject(config);
    if (new File(Writer.ERROR).exists()) {
      return new Report(fix, bank.compare());
    }
    return Report.empty(fix);
  }

  public Report effectByScope(Fix fix, List<String> workList) {
    if (workList == null) {
      workList = new ArrayList<>();
    }
    workList.add(fix.className);
    Set<String> workSet = new HashSet<>(workList);
    AutoFixConfig.AutoFixConfigWriter config =
        new AutoFixConfig.AutoFixConfigWriter()
            .setLogError(true, true)
            .setMakeCallGraph(false)
            .setMakeFieldGraph(false)
            .setOptimized(false)
            .setMethodInheritanceTree(false)
            .setSuggest(true)
            .setWorkList(workSet.toArray(new String[0]));
    autoFixer.buildProject(config);
    if (new File(Writer.ERROR).exists()) {
      int totalEffect = 0;
      totalEffect += bank.compareByClass(fix.className, true);
      for (String clazz : workSet) {
        if (clazz.equals(fix.className)) {
          continue;
        }
        totalEffect += bank.compareByClass(clazz, false);
      }
      return new Report(fix, totalEffect);
    }
    return Report.empty(fix);
  }

  public abstract boolean isApplicable(Fix fix);

  public abstract boolean requiresInjection(Fix fix);
}
