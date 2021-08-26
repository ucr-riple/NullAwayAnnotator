package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.AutoFixer;
import edu.ucr.cs.riple.autofixer.Report;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.autofixer.metadata.CallUsageTracker;
import edu.ucr.cs.riple.autofixer.metadata.FieldUsageTracker;
import edu.ucr.cs.riple.autofixer.metadata.UsageTracker;
import edu.ucr.cs.riple.autofixer.metadata.graph.FixGraph;
import edu.ucr.cs.riple.autofixer.metadata.graph.SuperNode;
import edu.ucr.cs.riple.injector.Fix;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DeepExplorer extends BasicExplorer {

  private final CallUsageTracker callUsageTracker;
  private final FieldUsageTracker fieldUsageTracker;
  private final FixGraph<SuperNode> fixGraph;
  private Set<Report> reports;

  public DeepExplorer(AutoFixer autoFixer, Bank bank) {
    super(autoFixer, bank);
    this.callUsageTracker = autoFixer.callUsageTracker;
    this.fieldUsageTracker = autoFixer.fieldUsageTracker;
    this.fixGraph = new FixGraph<>(SuperNode::new);
  }

  private void init(){
    this.reports.forEach(report -> {
      Fix fix = report.fix;
      SuperNode node = fixGraph.findOrCreate(fix);
      node.effect = report.effectiveNess;
      node.usages.clear();
      switch (fix.location){
        case "CLASS_FIELD":
          node.updateUsages(fieldUsageTracker);
          break;
        case "METHOD_RETURN":
          node.updateUsages(callUsageTracker);
          break;
        case "METHOD_PARAM":
          node.usages.add(new UsageTracker.Usage(node.fix.method, node.fix.className));
          break;
      }
    });
  }

  public void start(List<Report> reports, int depth) {
    if (depth == 0) {
      return;
    }
    this.reports = reports.stream().filter(report -> report.effectiveNess > 0).collect(Collectors.toSet());
    init();

  }
}
