package edu.ucr.cs.riple.autofixer.explorers;

import com.uber.nullaway.autofix.AutoFixConfig;
import com.uber.nullaway.autofix.Writer;
import edu.ucr.cs.riple.autofixer.AutoFixer;
import edu.ucr.cs.riple.autofixer.FixIndex;
import edu.ucr.cs.riple.autofixer.FixType;
import edu.ucr.cs.riple.autofixer.Report;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.autofixer.metadata.UsageTracker;
import edu.ucr.cs.riple.autofixer.metadata.graph.FixGraph;
import edu.ucr.cs.riple.autofixer.metadata.graph.Node;
import edu.ucr.cs.riple.injector.Fix;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AdvancedExplorer extends BasicExplorer {

  final FixGraph<Node> fixGraph;
  protected UsageTracker tracker;
  protected final FixType fixType;

  public AdvancedExplorer(AutoFixer autoFixer, Bank bank, FixIndex fixIndex, FixType fixType) {
    super(autoFixer, bank, fixIndex);
    this.fixType = fixType;
    fixGraph = new FixGraph<>(Node::new);
    try {
      try (BufferedReader br = new BufferedReader(new FileReader(Writer.SUGGEST_FIX))) {
        String line;
        String delimiter = Writer.getDelimiterRegex();
        while ((line = br.readLine()) != null) {
          Fix fix = Fix.fromCSVLine(line, delimiter);
          if (isApplicable(fix)) {
            Node node = fixGraph.findOrCreate(fix);
            node.referred++;
          }
        }
      }
    } catch (IOException e) {
      System.err.println("Exception happened in initializing MethodParamExplorer...");
    }
    init();
    explore();
  }

  protected abstract void init();

  protected void explore() {
    HashMap<Integer, Set<Node>> groups = fixGraph.getGroups();
    System.out.println("Building for: " + groups.size() + " number of times");
    int i = 1;
    for (Set<Node> nodes : groups.values()) {
      System.out.println("Building: (Iteration " + i++ + " out of: " + groups.size() + ")");
      List<Fix> fixes = nodes.stream().map(node -> node.fix).collect(Collectors.toList());
      autoFixer.apply(fixes);
      AutoFixConfig.AutoFixConfigWriter writer =
          new AutoFixConfig.AutoFixConfigWriter()
              .setLogError(true, true)
              .setSuggest(true, AutoFixer.DEPTH > 0)
              .setWorkList(Collections.singleton("*"));
      autoFixer.buildProject(writer);
      bank.saveState(false, true);
      fixIndex.index();
      for (Node node : nodes) {
        int totalEffect = 0;
        for (UsageTracker.Usage usage : node.usages) {
          totalEffect += bank.compareByMethod(usage.clazz, usage.method, false);
          if (AutoFixer.DEPTH > 0) {
            node.updateTriggered(fixIndex.getByMethod(usage.clazz, usage.method));
          }
        }
        node.effect = totalEffect;
      }
      autoFixer.remove(fixes);
    }
  }

  protected Report predict(Fix fix) {
    Node node = fixGraph.find(fix);
    System.out.print(
        "Trying to predict: "
            + fix.className
            + " "
            + fix.method
            + " "
            + fix.param
            + " "
            + fix.location
            + ": ");
    if (node == null) {
      System.out.println("Not found...");
      return null;
    }
    System.out.println("Predicted...");
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
}
