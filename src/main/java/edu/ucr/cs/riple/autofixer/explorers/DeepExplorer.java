package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.AutoFixer;
import edu.ucr.cs.riple.autofixer.Report;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.autofixer.metadata.CompoundTracker;
import edu.ucr.cs.riple.autofixer.metadata.graph.FixGraph;
import edu.ucr.cs.riple.autofixer.metadata.graph.SuperNode;
import edu.ucr.cs.riple.injector.Fix;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DeepExplorer extends BasicExplorer {

  private final CompoundTracker tracker;
  private final FixGraph<SuperNode> fixGraph;
  private Set<Report> reports;

  public DeepExplorer(AutoFixer autoFixer, Bank bank) {
    super(autoFixer, bank);
    this.tracker = new CompoundTracker(autoFixer.fieldUsageTracker, autoFixer.callUsageTracker);
    this.fixGraph = new FixGraph<>(SuperNode::new);
  }

  private void init() {
    this.reports.forEach(
        report -> {
          Fix fix = report.fix;
          SuperNode node = fixGraph.findOrCreate(fix);
          node.effect = report.effectiveNess;
          node.updateUsages(tracker);
        });
  }

  public void start(List<Report> reports, int depth) {
    if (depth == 0) {
      return;
    }
    this.reports =
        reports.stream().filter(report -> report.effectiveNess > 0).collect(Collectors.toSet());
    init();
  }
}
