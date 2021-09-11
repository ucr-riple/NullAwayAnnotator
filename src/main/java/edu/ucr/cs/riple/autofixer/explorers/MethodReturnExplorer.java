package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.AutoFixer;
import edu.ucr.cs.riple.autofixer.FixType;
import edu.ucr.cs.riple.autofixer.Report;
import edu.ucr.cs.riple.autofixer.metadata.index.Bank;
import edu.ucr.cs.riple.autofixer.metadata.index.Error;
import edu.ucr.cs.riple.autofixer.metadata.index.FixEntity;
import edu.ucr.cs.riple.injector.Fix;

public class MethodReturnExplorer extends AdvancedExplorer {

  public MethodReturnExplorer(AutoFixer autoFixer, Bank<Error> errorBank, Bank<FixEntity> fixBank) {
    super(autoFixer, errorBank, fixBank, FixType.METHOD_RETURN);
  }

  @Override
  protected void init() {
    tracker = autoFixer.callUsageTracker;
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

  @Override
  public boolean requiresInjection(Fix fix) {
    return true;
  }
}
