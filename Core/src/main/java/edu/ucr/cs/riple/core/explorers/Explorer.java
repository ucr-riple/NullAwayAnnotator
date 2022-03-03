package edu.ucr.cs.riple.core.explorers;

import edu.ucr.cs.riple.core.AutoFixer;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.metadata.index.Bank;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.FixEntity;
import edu.ucr.cs.riple.core.nullaway.AutoFixConfig;
import edu.ucr.cs.riple.core.nullaway.Writer;
import edu.ucr.cs.riple.injector.Fix;
import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class Explorer {

  protected final AutoFixer autoFixer;
  protected final Bank<Error> errorBank;
  protected final Bank<FixEntity> fixBank;

  public Explorer(AutoFixer autoFixer, Bank<Error> errorBank, Bank<FixEntity> fixBank) {
    this.autoFixer = autoFixer;
    this.errorBank = errorBank;
    this.fixBank = fixBank;
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
            .setAnnots(AutoFixer.NULLABLE_ANNOT, "UNKNOWN")
            .setWorkList(Collections.singleton("*"));
    autoFixer.buildProject(config);
    if (new File(Writer.ERROR).exists()) {
      return new Report(fix, errorBank.compare());
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
            .setAnnots(AutoFixer.NULLABLE_ANNOT, "UNKNOWN")
            .setWorkList(workSet);
    autoFixer.buildProject(config);
    if (new File(Writer.ERROR).exists()) {
      int totalEffect = 0;
      totalEffect += errorBank.compareByClass(fix.className, true).size;
      for (String clazz : workSet) {
        if (clazz.equals(fix.className)) {
          continue;
        }
        totalEffect += errorBank.compareByClass(clazz, false).size;
      }
      return new Report(fix, totalEffect);
    }
    return Report.empty(fix);
  }

  public abstract boolean isApplicable(Fix fix);

  public abstract boolean requiresInjection(Fix fix);
}
