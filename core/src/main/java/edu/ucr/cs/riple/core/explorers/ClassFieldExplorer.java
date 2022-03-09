package edu.ucr.cs.riple.core.explorers;

import edu.ucr.cs.riple.core.Annotator;
import edu.ucr.cs.riple.core.FixType;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.metadata.index.Bank;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.FixEntity;
import edu.ucr.cs.riple.injector.Fix;
import java.util.List;

public class ClassFieldExplorer extends AdvancedExplorer {

  public ClassFieldExplorer(
      Annotator annotator, List<Fix> fixes, Bank<Error> errorBank, Bank<FixEntity> fixBank) {
    super(annotator, fixes, errorBank, fixBank, FixType.CLASS_FIELD);
  }

  @Override
  protected void init() {
    this.tracker = annotator.fieldUsageTracker;
    System.out.println("Trying to find groups for Class Field fixes");
    fixGraph.updateUsages(tracker);
    fixGraph.findGroups();
  }

  @Override
  protected Report effectByScope(Fix fix) {
    return super.effectByScope(fix, tracker.getUsers(fix));
  }

  @Override
  public boolean isApplicable(Fix fix) {
    return fix.location.equals(fixType.name);
  }
}
