package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.Diagnose;
import edu.ucr.cs.riple.autofixer.DiagnoseReport;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.autofixer.metadata.FixGraph;
import edu.ucr.cs.riple.autofixer.metadata.MethodInheritanceTree;
import edu.ucr.cs.riple.autofixer.metadata.MethodNode;
import edu.ucr.cs.riple.autofixer.nullaway.AutoFixConfig;
import edu.ucr.cs.riple.injector.Fix;
import java.util.List;
import java.util.stream.Collectors;

public class MethodParamExplorer extends AdvancedExplorer {

  private MethodInheritanceTree mit;

  public MethodParamExplorer(Diagnose diagnose, Bank bank) {
    super(diagnose, bank);
  }

  @Override
  protected void init() {
    mit = diagnose.methodInheritanceTree;
  }

  @Override
  protected void explore() {
    int maxsize = MethodInheritanceTree.maxParamSize();
    System.out.println("Max size for method parameter list is: " + maxsize);
    for (int i = 0; i < maxsize; i++) {
      System.out.println("Analyzing params at index: " + i + " for all methods...");
      AutoFixConfig.AutoFixConfigWriter config =
          new AutoFixConfig.AutoFixConfigWriter()
              .setLogError(true, true)
              .setSuggest(true)
              .setMethodParamTest(true, i);
      diagnose.buildProject(config);
      bank.saveState(false, true);
      for (List<FixGraph.Node> list : fixGraph.nodes.values()) {
        int finalI = i;
        for (FixGraph.Node node :
            list.stream()
                .filter(node -> Integer.parseInt(node.fix.index) == finalI)
                .collect(Collectors.toList())) {
          int localEffect = bank.compareByMethod(node.fix.className, node.fix.method, false);
          node.effect = localEffect + calculateInheritanceViolationError(node, i);
        }
      }
    }
    System.out.println("Captured all methods behavior against nullability of parameter.");
  }

  @Override
  protected DiagnoseReport predict(Fix fix) {
    FixGraph.Node node = fixGraph.find(fix);
    if (node == null) {
      return null;
    }
    return new DiagnoseReport(fix, node.effect - node.referred);
  }

  @Override
  protected DiagnoseReport effectByScope(Fix fix) {
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
    boolean[] thisMethodFlag = mit.findNode(node.fix.method, node.fix.className).annotFlags;
    if (index >= thisMethodFlag.length) {
      return 0;
    }
    for (MethodNode subMethod : mit.getSubMethods(node.fix.method, node.fix.className, false)) {
      if (!thisMethodFlag[index]) {
        if (!subMethod.annotFlags[index]) {
          effect++;
        }
      }
    }
    List<MethodNode> superMethods = mit.getSuperMethods(node.fix.method, node.fix.className, false);
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
