package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.AutoFixer;
import edu.ucr.cs.riple.autofixer.Report;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.autofixer.metadata.CallUsageTracker;
import edu.ucr.cs.riple.autofixer.metadata.FieldUsageTracker;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeepExplorer extends BasicExplorer {

  private final CallUsageTracker callUsageTracker;
  private final FieldUsageTracker fieldUsageTracker;
  private Set<Report> reports;

  public DeepExplorer(AutoFixer autoFixer, Bank bank) {
    super(autoFixer, bank);
    this.callUsageTracker = autoFixer.callUsageTracker;
    this.fieldUsageTracker = autoFixer.fieldUsageTracker;
  }

  public void start(List<Report> reports, int depth) {
    if (depth == 0) {
      return;
    }
    this.reports = new HashSet<>(reports);
  }
}
