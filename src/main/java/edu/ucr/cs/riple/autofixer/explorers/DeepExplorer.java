package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.AutoFixer;
import edu.ucr.cs.riple.autofixer.Report;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.autofixer.metadata.CallUsageTracker;
import edu.ucr.cs.riple.autofixer.metadata.FieldUsageTracker;
import java.util.List;

public class DeepExplorer extends BasicExplorer {

  private final CallUsageTracker callUsageTracker;
  private final FieldUsageTracker fieldUsageTracker;

  public DeepExplorer(AutoFixer autoFixer, Bank bank) {
    super(autoFixer, bank);
    this.callUsageTracker = autoFixer.callUsageTracker;
    this.fieldUsageTracker = autoFixer.fieldUsageTracker;
  }

  public void setup(List<Report> reports) {}
}
