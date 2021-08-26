package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.AutoFixer;
import edu.ucr.cs.riple.autofixer.FixIndex;
import edu.ucr.cs.riple.autofixer.FixType;
import edu.ucr.cs.riple.autofixer.Report;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.injector.Fix;

public class ClassFieldExplorer extends AdvancedExplorer {

  public ClassFieldExplorer(AutoFixer autoFixer, Bank bank, FixIndex fixIndex) {
    super(autoFixer, bank, fixIndex, FixType.CLASS_FIELD);
  }

  @Override
  protected void init() {
    this.tracker = autoFixer.fieldUsageTracker;
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

  @Override
  public boolean requiresInjection(Fix fix) {
    return true;
  }
}
