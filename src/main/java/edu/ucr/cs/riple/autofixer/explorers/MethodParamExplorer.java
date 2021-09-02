package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.AutoFixer;
import edu.ucr.cs.riple.autofixer.FixIndex;
import edu.ucr.cs.riple.autofixer.FixType;
import edu.ucr.cs.riple.autofixer.Report;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.autofixer.metadata.MethodInheritanceTree;
import edu.ucr.cs.riple.autofixer.metadata.MethodNode;
import edu.ucr.cs.riple.autofixer.metadata.graph.Node;
import edu.ucr.cs.riple.autofixer.nullaway.AutoFixConfig;
import edu.ucr.cs.riple.injector.Fix;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MethodParamExplorer extends AdvancedExplorer {

  private MethodInheritanceTree mit;

  public MethodParamExplorer(AutoFixer autoFixer, Bank bank, FixIndex fixIndex) {
    super(autoFixer, bank, fixIndex, FixType.METHOD_PARAM);
  }

  @Override
  protected void init() {
    mit = autoFixer.methodInheritanceTree;
  }

  @Override
  protected void explore() {
    int maxsize = MethodInheritanceTree.maxParamSize();
    System.out.println("Max size for method parameter list is: " + maxsize);
    List<Node> allNodes = new ArrayList<>();
    for (Set<Node> subList : fixGraph.nodes.values()) {
      allNodes.addAll(subList);
    }
    for (int i = 0; i < maxsize; i++) {
      System.out.println(
          "Analyzing params at index: (" + (i + 1) + " out of " + maxsize + ") for all methods...");
      int finalI1 = i;
      List<Node> subList =
          allNodes
              .stream()
              .filter(node -> node.fix.index.equals(finalI1 + ""))
              .collect(Collectors.toList());
      if (subList.size() == 0) {
        System.out.println("No fix at this index, skipping...");
        continue;
      }
      AutoFixConfig.AutoFixConfigWriter config =
          new AutoFixConfig.AutoFixConfigWriter()
              .setLogError(true, true)
              .setSuggest(true, false)
              .setAnnots(AutoFixer.NULLABLE_ANNOT, "UNKNOWN")
              .setMethodParamTest(true, (long) i);
      autoFixer.buildProject(config);
      bank.saveState(false, true);
      fixIndex.index();
      for (Node node : subList) {
        int localEffect = bank.compareByMethod(node.fix.className, node.fix.method, false);
        node.effect = localEffect + calculateInheritanceViolationError(node, i);
        if (AutoFixer.DEPTH > 0) {
          node.updateTriggered(fixIndex.getByMethod(node.fix.className, node.fix.method));
        }
      }
    }
    System.out.println("Captured all methods behavior against nullability of parameter.");
  }

  @Override
  protected Report predict(Fix fix) {
    Node node = fixGraph.find(fix);
    System.out.print(
        "Trying to predict: "
            + fix.className
            + " "
            + fix.method
            + " "
            + fix.param
            + " METHOD_PARAM: ");
    if (node == null) {
      System.out.println("Not found...");
      return null;
    }
    System.out.println("Predicted...");
    Report report = new Report(fix, node.effect - node.referred);
    report.triggered = node.triggered;
    return report;
  }

  @Override
  protected Report effectByScope(Fix fix) {
    return effectByScope(fix, null);
  }

  @Override
  public boolean isApplicable(Fix fix) {
    return fix.location.equals(fixType.name);
  }

  @Override
  public boolean requiresInjection(Fix fix) {
    return fixGraph.find(fix) == null;
  }

  private int calculateInheritanceViolationError(Node node, int index) {
    int effect = 0;
    Fix fix = node.fix;
    boolean[] thisMethodFlag = mit.findNode(fix.method, fix.className).annotFlags;
    if (index >= thisMethodFlag.length) {
      return 0;
    }
    for (MethodNode subMethod : mit.getSubMethods(fix.method, fix.className, false)) {
      if (!thisMethodFlag[index]) {
        if (!subMethod.annotFlags[index]) {
          effect++;
        }
      }
    }
    List<MethodNode> superMethods = mit.getSuperMethods(fix.method, fix.className, false);
    if (superMethods.size() != 0) {
      MethodNode superMethod = superMethods.get(0);
      if (!thisMethodFlag[index]) {
        if (superMethod.annotFlags[index]) {
          effect--;
        }
      }
    }
    return effect;
  }
}
