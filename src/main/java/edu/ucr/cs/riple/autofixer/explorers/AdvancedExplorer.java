package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.AutoFixer;
import edu.ucr.cs.riple.autofixer.Report;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.autofixer.metadata.FixGraph;
import edu.ucr.cs.riple.autofixer.metadata.UsageTracker;
import edu.ucr.cs.riple.autofixer.nullaway.AutoFixConfig;
import edu.ucr.cs.riple.autofixer.nullaway.Writer;
import edu.ucr.cs.riple.injector.Fix;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AdvancedExplorer extends BasicExplorer {

  final FixGraph fixGraph;
  protected UsageTracker tracker;

  public AdvancedExplorer(AutoFixer autoFixer, Bank bank) {
    super(autoFixer, bank);
    fixGraph = new FixGraph();
    try {
      try (BufferedReader br = new BufferedReader(new FileReader(Writer.SUGGEST_FIX))) {
        String line;
        String delimiter = Writer.getDelimiterRegex();
        while ((line = br.readLine()) != null) {
          Fix fix = Fix.fromCSVLine(line, delimiter);
          if (isApplicable(fix)) {
            FixGraph.Node node = fixGraph.findOrCreate(fix);
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
    HashMap<Integer, List<FixGraph.Node>> groups = fixGraph.getGroups();
    System.out.println("Building for: " + groups.size() + " number of times");
    for (List<FixGraph.Node> nodes : groups.values()) {
      Set<String> workList = new HashSet<>();
      System.out.println("Building..");
      List<Fix> fixes = nodes.stream().map(node -> node.fix).collect(Collectors.toList());
      for (Fix fix : fixes) {
        workList.addAll(tracker.getUsers(fix));
        workList.add(fix.className);
      }
      autoFixer.apply(fixes);
      AutoFixConfig.AutoFixConfigWriter writer =
          new AutoFixConfig.AutoFixConfigWriter()
              .setWorkList(workList.toArray(new String[0]))
              .setLogError(true, true)
              .setSuggest(true);
      autoFixer.buildProject(writer);
      bank.saveState(true, true);
      for (FixGraph.Node node : nodes) {
        int totalEffect = 0;
        for (UsageTracker.Usage usage : node.usages) {
          if (usage.method == null || usage.method.equals("null")) {
            totalEffect += bank.compareByClass(usage.clazz, false);
          } else {
            totalEffect += bank.compareByMethod(usage.clazz, usage.method, false);
          }
        }
        node.effect = totalEffect;
      }
      autoFixer.remove(fixes);
    }
  }

  protected Report predict(Fix fix) {
    FixGraph.Node node = fixGraph.find(fix);
    if (node == null) {
      return null;
    }
    return new Report(fix, node.effect);
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
