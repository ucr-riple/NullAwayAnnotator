package edu.ucr.cs.riple.core.explorers;

import com.uber.nullaway.fixserialization.FixSerializationConfig;
import edu.ucr.cs.riple.core.AutoFixer;
import edu.ucr.cs.riple.core.FixType;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.metadata.graph.FixGraph;
import edu.ucr.cs.riple.core.metadata.graph.Node;
import edu.ucr.cs.riple.core.metadata.index.Bank;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.FixEntity;
import edu.ucr.cs.riple.core.metadata.index.Result;
import edu.ucr.cs.riple.core.metadata.trackers.Usage;
import edu.ucr.cs.riple.core.metadata.trackers.UsageTracker;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.Fix;
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
    System.out.println("Scheduling for: " + groups.size() + " builds");
    ProgressBar pb = Utility.createProgressBar("Exploring " + fixType.name, groups.values().size());
    for (Set<Node> nodes : groups.values()) {
      pb.step();
      pb.setExtraMessage("Gathering fixes");
      List<Fix> fixes = nodes.stream().map(node -> node.fix).collect(Collectors.toList());
      pb.setExtraMessage("Applying fixes");
      autoFixer.apply(fixes);
      pb.setExtraMessage("Building");
      FixSerializationConfig.Builder config =
          new FixSerializationConfig.Builder()
              .setSuggest(true, AutoFixer.DEPTH > 0)
              .setAnnotations(AutoFixer.NULLABLE_ANNOT, "UNKNOWN");
      autoFixer.buildProject(config);
      pb.setExtraMessage("Saving state");
      errorBank.saveState(false, true);
      fixBank.saveState(true, true);
      int index = 0;
      for (Node node : nodes) {
        pb.setExtraMessage("Node number: " + (index++) + " / " + nodes.size());
        int totalEffect = 0;
        for (Usage usage : node.usages) {
          Result<Error> errorComparison =
              errorBank.compareByMethod(usage.clazz, usage.method, false);
          node.analyzeStatus(errorComparison.dif);
          totalEffect += errorComparison.size;
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
    report.finished = node.finished;
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
