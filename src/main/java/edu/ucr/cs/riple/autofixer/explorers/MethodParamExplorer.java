package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.AutoFixer;
import edu.ucr.cs.riple.autofixer.Report;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.autofixer.metadata.FixGraph;
import edu.ucr.cs.riple.autofixer.metadata.MethodInheritanceTree;
import edu.ucr.cs.riple.autofixer.metadata.MethodNode;
import edu.ucr.cs.riple.autofixer.nullaway.AutoFixConfig;
import edu.ucr.cs.riple.injector.Fix;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MethodParamExplorer extends AdvancedExplorer {

  private MethodInheritanceTree mit;

  public MethodParamExplorer(AutoFixer autoFixer, Bank bank) {
    super(autoFixer, bank);
  }

  @Override
  protected void init() {
    mit = autoFixer.methodInheritanceTree;
  }

  @Override
  protected void explore() {
    int maxsize = MethodInheritanceTree.maxParamSize();
    System.out.println("Max size for method parameter list is: " + maxsize);
    List<FixGraph.Node> allNodes = new ArrayList<>();
    for (List<FixGraph.Node> subList : fixGraph.nodes.values()) {
      allNodes.addAll(subList);
    }
    for (int i = 0; i < maxsize; i++) {
      System.out.println("Analyzing params at index: (" + (i+1) + " out of " + maxsize + ") for all methods...");
      int finalI1 = i;
      List<FixGraph.Node> subList =
          allNodes
              .stream()
              .filter(node -> node.fix.index.equals(finalI1 + ""))
              .collect(Collectors.toList());
      List<Fix> appliedFixes = subList.stream().map(node -> node.fix).collect(Collectors.toList());
      if (appliedFixes.size() == 0) {
        System.out.println("No fix at this index, skipping...");
        continue;
      }
      autoFixer.apply(appliedFixes);
      AutoFixConfig.AutoFixConfigWriter config =
          new AutoFixConfig.AutoFixConfigWriter()
              .setLogError(true, true)
              .setSuggest(true)
              .setMethodParamTest(true);
      autoFixer.buildProject(config);
      bank.saveState(false, true);
      for (FixGraph.Node node : subList) {
        int localEffect = bank.compareByMethod(node.fix.className, node.fix.method, false);
        node.effect = localEffect + calculateInheritanceViolationError(node, i);
      }
      autoFixer.remove(appliedFixes);
    }
    System.out.println("Captured all methods behavior against nullability of parameter.");
  }

  @Override
  protected Report predict(Fix fix) {
    FixGraph.Node node = fixGraph.find(fix);
    System.out.print("Trying to predict: " + fix.className + " " + fix.method + " " + fix.param + " METHOD_PARAM: ");
    if (node == null) {
      System.out.println("Not found...");
      return null;
    }
    System.out.println("Predicted...");
    return new Report(fix, node.effect - node.referred);
  }

  @Override
  protected Report effectByScope(Fix fix) {
    return effectByScope(fix, null);
  }

  @Override
  public boolean isApplicable(Fix fix) {
    return fix.location.equals("METHOD_PARAM");
  }

  @Override
  public boolean requiresInjection(Fix fix) {
    return fixGraph.find(fix) == null;
  }

  private int calculateInheritanceViolationError(FixGraph.Node node, int index) {
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
