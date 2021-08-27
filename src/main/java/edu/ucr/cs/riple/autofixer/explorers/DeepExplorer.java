package edu.ucr.cs.riple.autofixer.explorers;

import com.uber.nullaway.autofix.AutoFixConfig;
import edu.ucr.cs.riple.autofixer.AutoFixer;
import edu.ucr.cs.riple.autofixer.FixIndex;
import edu.ucr.cs.riple.autofixer.Report;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.autofixer.metadata.CompoundTracker;
import edu.ucr.cs.riple.autofixer.metadata.UsageTracker;
import edu.ucr.cs.riple.autofixer.metadata.graph.FixGraph;
import edu.ucr.cs.riple.autofixer.metadata.graph.Node;
import edu.ucr.cs.riple.autofixer.metadata.graph.SuperNode;
import edu.ucr.cs.riple.injector.Fix;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DeepExplorer extends BasicExplorer {

  private final CompoundTracker tracker;
  private final FixGraph<SuperNode> fixGraph;
  private Set<Report> reports;

  public DeepExplorer(AutoFixer autoFixer, Bank bank, FixIndex fixIndex) {
    super(autoFixer, bank, fixIndex);
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
          node.report = report;
          node.triggered = report.triggered;
        });
  }

  public void start(List<Report> reports) {
    if (AutoFixer.DEPTH == 0) {
      return;
    }
    this.reports =
        reports.stream().filter(report -> report.effectiveNess > 0).collect(Collectors.toSet());
    init();
    int currentDepth = 0;
    while (fixGraph.nodes.size() > 0 && currentDepth < AutoFixer.DEPTH) {
      explore();
      currentDepth++;

    }
  }

  private void explore() {
    fixGraph.findGroups();
    HashMap<Integer, Set<SuperNode>> groups = fixGraph.getGroups();
    System.out.println("Building for: " + groups.size() + " number of times");
    int i = 1;
    for (Set<SuperNode> group : groups.values()) {
      System.out.println("Building: (Iteration " + i++ + " out of: " + groups.size() + ")");
      List<Fix> fixes = new ArrayList<>();
      group.forEach(SuperNode::mergeTriggered);
      group.forEach(e -> fixes.addAll(e.getFixChain()));
      autoFixer.apply(fixes);
      bank.saveState(true, true);
      AutoFixConfig.AutoFixConfigWriter config =
          new AutoFixConfig.AutoFixConfigWriter().setLogError(true, true).setSuggest(true, true);
      autoFixer.buildProject(config);
      fixIndex.index();
      group.forEach(
          superNode -> {
            for (Node node : superNode.followUps) {
              int totalEffect = 0;
              for (UsageTracker.Usage usage : node.usages) {
                totalEffect += bank.compareByMethod(usage.clazz, usage.method, false);
                node.updateTriggered(fixIndex.getByMethod(usage.clazz, usage.method));
              }
              node.effect = totalEffect;
            }
          });
      autoFixer.remove(fixes);
    }
  }
}
