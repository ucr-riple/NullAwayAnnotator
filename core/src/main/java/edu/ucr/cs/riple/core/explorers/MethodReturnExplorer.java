package edu.ucr.cs.riple.core.explorers;

import edu.ucr.cs.riple.core.Annotator;
import edu.ucr.cs.riple.core.FixType;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.metadata.index.Bank;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.FixEntity;
import edu.ucr.cs.riple.injector.Fix;
import java.util.List;

public class MethodReturnExplorer extends AdvancedExplorer {

  public MethodReturnExplorer(
      Annotator annotator, List<Fix> fixes, Bank<Error> errorBank, Bank<FixEntity> fixBank) {
    super(annotator, fixes, errorBank, fixBank, FixType.METHOD_RETURN);
  }

  @Override
  protected void init() {
    tracker = annotator.callUsageTracker;
    System.out.println("Trying to find groups for Method Return fixes");
    fixGraph.updateUsages(tracker);
    fixGraph.findGroups();
  }

  @Override
  protected Report effectByScope(Fix fix) {
    return super.effectByScope(fix, tracker.getUsers(fix));
  }

  @Override
  public boolean isApplicable(Fix fix) {
    return fix.location.equals(FixType.METHOD_RETURN.name);
  }
}
