package edu.ucr.cs.riple.autofixer.explorers;

import com.uber.nullaway.autofix.AutoFixConfig;
import com.uber.nullaway.autofix.Writer;
import edu.ucr.cs.riple.autofixer.AutoFixer;
import edu.ucr.cs.riple.autofixer.Report;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.injector.Fix;
import java.io.File;
import java.util.Collections;
import java.util.HashSet;
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
            .setSuggest(true, false)
            .setWorkList(Collections.singleton("*"));
    autoFixer.buildProject(config);
    if (new File(Writer.ERROR).exists()) {
      return new Report(fix, bank.compare());
    }
    return Report.empty(fix);
  }

  public Report effectByScope(Fix fix, Set<String> workSet) {
    if (workSet == null) {
      workSet = new HashSet<>();
    }
    workSet.add(fix.className);
    AutoFixConfig.AutoFixConfigWriter config =
        new AutoFixConfig.AutoFixConfigWriter()
            .setLogError(true, true)
            .setMakeCallGraph(false)
            .setMakeFieldGraph(false)
            .setOptimized(false)
            .setMethodInheritanceTree(false)
            .setSuggest(true, false)
            .setWorkList(workSet);
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
