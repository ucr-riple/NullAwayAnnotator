package edu.ucr.cs.riple.core.explorers;

import com.uber.nullaway.fixserialization.FixSerializationConfig;
import edu.ucr.cs.riple.core.AutoFixer;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.metadata.index.Bank;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.FixEntity;
import edu.ucr.cs.riple.injector.Fix;
import java.io.File;
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
    FixSerializationConfig.Builder config =
        new FixSerializationConfig.Builder()
            .setSuggest(true, false)
            .setAnnotations(AutoFixer.NULLABLE_ANNOT, "UNKNOWN");
    autoFixer.buildProject(config);
    if (new File(AutoFixer.ERROR_NAME).exists()) {
      return new Report(fix, errorBank.compare());
    }
    return Report.empty(fix);
  }

  public Report effectByScope(Fix fix, Set<String> workSet) {
    if (workSet == null) {
      workSet = new HashSet<>();
    }
    workSet.add(fix.className);
    FixSerializationConfig.Builder config =
        new FixSerializationConfig.Builder()
            .setSuggest(true, false)
            .setAnnotations(AutoFixer.NULLABLE_ANNOT, "UNKNOWN");
    autoFixer.buildProject(config);
    if (new File(AutoFixer.ERROR_NAME).exists()) {
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
