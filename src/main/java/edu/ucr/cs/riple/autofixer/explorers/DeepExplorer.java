package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.AutoFixer;
import edu.ucr.cs.riple.autofixer.Report;
import edu.ucr.cs.riple.autofixer.metadata.graph.FixGraph;
import edu.ucr.cs.riple.autofixer.metadata.graph.SuperNode;
import edu.ucr.cs.riple.autofixer.metadata.index.Bank;
import edu.ucr.cs.riple.autofixer.metadata.index.Error;
import edu.ucr.cs.riple.autofixer.metadata.index.FixEntity;
import edu.ucr.cs.riple.autofixer.metadata.trackers.CompoundTracker;
import edu.ucr.cs.riple.autofixer.metadata.trackers.Usage;
import edu.ucr.cs.riple.autofixer.nullaway.AutoFixConfig;
import edu.ucr.cs.riple.autofixer.util.Utility;
import edu.ucr.cs.riple.injector.Fix;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import me.tongfei.progressbar.ProgressBar;

public class DeepExplorer extends BasicExplorer {

  private final CompoundTracker tracker;
  private final FixGraph<SuperNode> fixGraph;

  public DeepExplorer(AutoFixer autoFixer, Bank<Error> errorBank, Bank<FixEntity> fixBank) {
    super(autoFixer, errorBank, fixBank);
    this.tracker = new CompoundTracker(autoFixer.fieldUsageTracker, autoFixer.callUsageTracker);
    this.fixGraph = new FixGraph<>(SuperNode::new);
  }

  private void init(List<Report> reports) {
    this.fixGraph.clear();
    Set<Report> filteredReports =
        reports.stream().filter(report -> report.effectiveNess > 0).collect(Collectors.toSet());
    System.out.println(
        "Number of unfinished reports with effectiveness greater than zero: "
            + filteredReports.size());
    filteredReports.forEach(
        report -> {
          Fix fix = report.fix;
          SuperNode node = fixGraph.findOrCreate(fix);
          node.effect = report.effectiveNess;
          node.report = report;
          node.triggered = report.triggered;
          node.followUps.addAll(report.followups);
          node.mergeTriggered();
          node.updateUsages(tracker);
        });
  }

  public void start(List<Report> reports) {
    if (AutoFixer.DEPTH == 0) {
      return;
    }
    System.out.println(
        "Deep explorer is active...\nAnalysing for " + AutoFixer.DEPTH + " number of levels");
    for (int i = 0; i < AutoFixer.DEPTH; i++) {
      System.out.println("Analyzing at level " + i);
      init(reports);
      explore();
      List<SuperNode> nodes = fixGraph.getAllNodes();
      nodes.forEach(
          superNode -> {
            Report report = superNode.report;
            report.effectiveNess = superNode.effect;
            report.followups = superNode.followUps;
          });
    }
  }

  private void explore() {
    if (fixGraph.nodes.size() == 0) {
      return;
    }
    fixGraph.findGroups();
    HashMap<Integer, Set<SuperNode>> groups = fixGraph.getGroups();
    System.out.println("Building for: " + groups.size() + " number of times");
    ProgressBar pb = Utility.createProgressBar("Deep analysis", groups.size());
    for (Set<SuperNode> group : groups.values()) {
      pb.step();
      List<Fix> fixes = new ArrayList<>();
      group.forEach(superNode -> fixes.addAll(superNode.getFixChain()));
      pb.setExtraMessage("Applying fixes");
      autoFixer.apply(fixes);
      AutoFixConfig.AutoFixConfigWriter config =
          new AutoFixConfig.AutoFixConfigWriter()
              .setLogError(true, true)
              .setSuggest(true, true)
              .setAnnots(AutoFixer.NULLABLE_ANNOT, "UNKNOWN")
              .setInheritanceCheckDisabled(true)
              .setWorkList(Collections.singleton("*"));
      pb.setExtraMessage("Building");
      autoFixer.buildProject(config);
      pb.setExtraMessage("Saving state");
      errorBank.saveState(false, true);
      fixBank.saveState(false, true);
      group.forEach(
          superNode -> {
            int totalEffect = 0;
            for (Usage usage : superNode.usages) {
              totalEffect += errorBank.compareByMethod(usage.clazz, usage.method, false).effect;
              superNode.updateTriggered(
                  fixBank
                      .compareByMethod(usage.clazz, usage.method, false)
                      .dif
                      .stream()
                      .map(fixEntity -> fixEntity.fix)
                      .collect(Collectors.toList()));
            }
            superNode.setEffect(totalEffect, autoFixer.methodInheritanceTree);
          });
      autoFixer.remove(fixes);
    }
    pb.step();
  }
}
