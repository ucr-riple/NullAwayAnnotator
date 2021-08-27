package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.AutoFixer;
import edu.ucr.cs.riple.autofixer.Report;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.injector.Fix;

public class MethodReturnExplorer extends AdvancedExplorer {

  public MethodReturnExplorer(AutoFixer autoFixer, Bank bank) {
    super(autoFixer, bank);
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
    return fix.location.equals("METHOD_RETURN");
  }

  @Override
  public boolean requiresInjection(Fix fix) {
    return true;
  }
}
