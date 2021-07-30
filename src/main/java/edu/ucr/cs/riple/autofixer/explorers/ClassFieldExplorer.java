package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.Diagnose;
import edu.ucr.cs.riple.autofixer.DiagnoseReport;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.autofixer.metadata.FieldUsageTracker;
import edu.ucr.cs.riple.injector.Fix;

public class ClassFieldExplorer extends AdvancedExplorer {

  private FieldUsageTracker fieldUsageTracker;

  public ClassFieldExplorer(Diagnose diagnose, Bank bank) {
    super(diagnose, bank);
  }

  @Override
  protected void init() {
    this.fieldUsageTracker = diagnose.fieldUsageTracker;
    System.out.println("Trying to find groups for Class Field fixes");
    fixGraph.findGroups(fieldUsageTracker);
  }

  @Override
  protected void explore() {}

  @Override
  protected DiagnoseReport effectByScope(Fix fix) {
    return super.effectByScope(fix, fieldUsageTracker.getUsers(fix));
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
