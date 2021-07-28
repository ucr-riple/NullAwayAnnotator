package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.Diagnose;
import edu.ucr.cs.riple.autofixer.DiagnoseReport;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.autofixer.metadata.FieldUsage;
import edu.ucr.cs.riple.injector.Fix;

public class ClassFieldExplorer extends AdvancedExplorer {

  private FieldUsage fieldUsage;

  public ClassFieldExplorer(Diagnose diagnose, Bank bank) {
    super(diagnose, bank);
  }

  @Override
  protected void init() {
    this.fieldUsage = diagnose.fieldUsage;
    fixGraph.findGroups(fieldUsage);
  }

  @Override
  protected void explore() {}

  @Override
  protected DiagnoseReport effectByScope(Fix fix) {
    return super.effectByScope(fix, fieldUsage.getUserClassOfField(fix.param, fix.className));
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
