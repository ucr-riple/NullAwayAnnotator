package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.AutoFixer;
import edu.ucr.cs.riple.autofixer.Report;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.injector.Fix;

public class ClassFieldExplorer extends AdvancedExplorer {

  public ClassFieldExplorer(AutoFixer autoFixer, Bank bank) {
    super(autoFixer, bank);
  }

  @Override
  protected void init() {
    this.tracker = autoFixer.fieldUsageTracker;
    System.out.println("Trying to find groups for Class Field fixes");
    fixGraph.findGroups(tracker);
  }

  @Override
  protected Report effectByScope(Fix fix) {
    return super.effectByScope(fix, tracker.getUsers(fix));
  }

  @Override
  public boolean isApplicable(Fix fix) {
    return fix.location.equals("CLASS_FIELD");
  }

  @Override
  public boolean requiresInjection(Fix fix) {
    return true;
  }
}
