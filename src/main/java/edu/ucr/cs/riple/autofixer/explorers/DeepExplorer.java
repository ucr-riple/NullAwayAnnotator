package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.AutoFixer;
import edu.ucr.cs.riple.autofixer.Report;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.autofixer.metadata.CallUsageTracker;
import edu.ucr.cs.riple.autofixer.metadata.FieldUsageTracker;
import edu.ucr.cs.riple.injector.Fix;

public class DeepExplorer extends AdvancedExplorer {

  private CallUsageTracker callUsageTracker;
  private FieldUsageTracker fieldUsageTracker;

  public DeepExplorer(AutoFixer autoFixer, Bank bank) {
    super(autoFixer, bank);
  }

  @Override
  protected void init() {
    callUsageTracker = autoFixer.callUsageTracker;
    fieldUsageTracker = autoFixer.fieldUsageTracker;
    fixGraph.updateUsages(autoFixer.callUsageTracker);
    fixGraph.updateUsages(autoFixer.fieldUsageTracker);
  }

  @Override
  protected Report effectByScope(Fix fix) {
    return null;
  }
}
