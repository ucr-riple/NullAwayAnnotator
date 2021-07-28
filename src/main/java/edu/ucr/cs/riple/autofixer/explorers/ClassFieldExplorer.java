package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.Diagnose;
import edu.ucr.cs.riple.autofixer.DiagnoseReport;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.autofixer.metadata.FieldNode;
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
    ReversedFieldTracker reversed = new ReversedFieldTracker(fieldUsageTracker.filePath);
    fixGraph.findGroups(reversed);
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

  static class ReversedFieldTracker extends FieldUsageTracker {

    public ReversedFieldTracker(String filePath) {
      super(filePath);
    }

    @Override
    protected FieldNode addNodeByLine(String[] values) {
      return new FieldNode(values[2], values[3], values[0], values[1]);
    }
  }
}
