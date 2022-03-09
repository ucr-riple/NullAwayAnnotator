package edu.ucr.cs.riple.core.explorers;

import com.uber.nullaway.fixserialization.FixSerializationConfig;
import edu.ucr.cs.riple.core.Annotator;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.metadata.index.Bank;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.FixEntity;
import edu.ucr.cs.riple.injector.Fix;
import java.util.HashSet;
import java.util.Set;

public abstract class Explorer {

  protected final Annotator annotator;
  protected final Bank<Error> errorBank;
  protected final Bank<FixEntity> fixBank;

  public Explorer(Annotator annotator, Bank<Error> errorBank, Bank<FixEntity> fixBank) {
    this.annotator = annotator;
    this.errorBank = errorBank;
    this.fixBank = fixBank;
  }

  public Report effect(Fix fix) {
    FixSerializationConfig.Builder config =
        new FixSerializationConfig.Builder()
            .setSuggest(true, false)
            .setAnnotations(annotator.nullableAnnot, "UNKNOWN")
            .setOutputDirectory(annotator.dir.toString());
    ;
    annotator.buildProject(config);
    if (annotator.errorPath.toFile().exists()) {
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
            .setAnnotations(annotator.nullableAnnot, "UNKNOWN")
            .setOutputDirectory(annotator.dir.toString());
    ;
    annotator.buildProject(config);
    if (annotator.errorPath.toFile().exists()) {
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
