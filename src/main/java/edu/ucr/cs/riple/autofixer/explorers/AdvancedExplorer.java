package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.AutoFixer;
import edu.ucr.cs.riple.autofixer.FixType;
import edu.ucr.cs.riple.autofixer.Report;
import edu.ucr.cs.riple.autofixer.metadata.graph.FixGraph;
import edu.ucr.cs.riple.autofixer.metadata.graph.Node;
import edu.ucr.cs.riple.autofixer.metadata.index.Bank;
import edu.ucr.cs.riple.autofixer.metadata.index.Error;
import edu.ucr.cs.riple.autofixer.metadata.index.FixEntity;
import edu.ucr.cs.riple.autofixer.metadata.trackers.Usage;
import edu.ucr.cs.riple.autofixer.metadata.trackers.UsageTracker;
import edu.ucr.cs.riple.autofixer.nullaway.AutoFixConfig;
import edu.ucr.cs.riple.autofixer.util.Utility;
import edu.ucr.cs.riple.injector.Fix;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import me.tongfei.progressbar.ProgressBar;

public abstract class AdvancedExplorer extends BasicExplorer {

  final FixGraph<Node> fixGraph;
  protected UsageTracker tracker;
  protected final FixType fixType;

  public AdvancedExplorer(
      AutoFixer autoFixer,
      List<Fix> fixes,
      Bank<Error> errorBank,
      Bank<FixEntity> fixBank,
      FixType fixType) {
    super(autoFixer, errorBank, fixBank);
    this.fixType = fixType;
    this.fixGraph = new FixGraph<>(Node::new);
    fixes.forEach(
        fix -> {
          if (isApplicable(fix)) {
            fixGraph.findOrCreate(fix);
          }
        });
    if (fixGraph.nodes.size() > 0) {
      init();
      explore();
    }
  }

  protected abstract void init();

  protected void explore() {
    HashMap<Integer, Set<Node>> groups = fixGraph.getGroups();
    System.out.println("Building for: " + groups.size() + " number of times");
    ProgressBar pb = Utility.createProgressBar("Exploring " + fixType.name, groups.values().size());
    for (Set<Node> nodes : groups.values()) {
      pb.step();
      pb.setExtraMessage("Gathering fixes");
      List<Fix> fixes = nodes.stream().map(node -> node.fix).collect(Collectors.toList());
      pb.setExtraMessage("Applying fixes");
      autoFixer.apply(fixes);
      pb.setExtraMessage("Building");
      AutoFixConfig.AutoFixConfigWriter writer =
          new AutoFixConfig.AutoFixConfigWriter()
              .setLogError(true, true)
              .setSuggest(true, AutoFixer.DEPTH > 0)
              .setAnnots(AutoFixer.NULLABLE_ANNOT, "UNKNOWN")
              .setWorkList(Collections.singleton("*"));
      autoFixer.buildProject(writer);
      pb.setExtraMessage("Saving state");
      errorBank.saveState(false, true);
      fixBank.saveState(true, true);
      int index = 0;
      for (Node node : nodes) {
        pb.setExtraMessage("Node number: " + (index++) + " / " + nodes.size());
        int totalEffect = 0;
        for (Usage usage : node.usages) {
          totalEffect += errorBank.compareByMethod(usage.clazz, usage.method, false).effect;
          if (AutoFixer.DEPTH > 0) {
            node.updateTriggered(
                fixBank
                    .compareByMethod(usage.clazz, usage.method, false)
                    .dif
                    .stream()
                    .map(fixEntity -> fixEntity.fix)
                    .collect(Collectors.toList()));
          }
        }
        node.setEffect(totalEffect, autoFixer.methodInheritanceTree);
      }
      autoFixer.remove(fixes);
    }
    pb.close();
  }

  protected Report predict(Fix fix) {
    Node node = fixGraph.find(fix);
    if (node == null) {
      return null;
    }
    Report report = new Report(fix, node.effect);
    report.triggered = node.triggered;
    return report;
  }

  protected abstract Report effectByScope(Fix fix);

  @Override
  public Report effect(Fix fix) {
    Report report = predict(fix);
    if (report != null) {
      return report;
    }
    return effectByScope(fix);
  }

  @Override
  public boolean requiresInjection(Fix fix) {
    return fixGraph.find(fix) == null;
  }
}
