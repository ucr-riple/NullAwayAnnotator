package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.AutoFixer;
import edu.ucr.cs.riple.autofixer.FixType;
import edu.ucr.cs.riple.autofixer.Report;
import edu.ucr.cs.riple.autofixer.metadata.graph.Node;
import edu.ucr.cs.riple.autofixer.metadata.index.Bank;
import edu.ucr.cs.riple.autofixer.metadata.index.Error;
import edu.ucr.cs.riple.autofixer.metadata.index.FixEntity;
import edu.ucr.cs.riple.autofixer.metadata.index.Result;
import edu.ucr.cs.riple.autofixer.metadata.method.MethodInheritanceTree;
import edu.ucr.cs.riple.autofixer.nullaway.AutoFixConfig;
import edu.ucr.cs.riple.autofixer.util.Utility;
import edu.ucr.cs.riple.injector.Fix;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MethodParamExplorer extends AdvancedExplorer {

  private MethodInheritanceTree mit;

  public MethodParamExplorer(
      AutoFixer autoFixer, List<Fix> fixes, Bank<Error> errorBank, Bank<FixEntity> fixBank) {
    super(autoFixer, fixes, errorBank, fixBank, FixType.METHOD_PARAM);
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
      errorBank.saveState(false, true);
      fixBank.saveState(false, true);
      for (Node node : subList) {
        Result<Error> result =
            errorBank.compareByMethod(node.fix.className, node.fix.method, false);
        node.newErrors.addAll(result.dif);
        node.setEffect(
            result.effect + Utility.calculateInheritanceViolationError(this.mit, node.fix));
        if (AutoFixer.DEPTH > 0) {
          node.updateTriggered(
              fixBank
                  .compareByMethod(node.fix.className, node.fix.method, false)
                  .dif
                  .stream()
                  .map(fixEntity -> fixEntity.fix)
                  .collect(Collectors.toList()));
        }
      }
    }
    System.out.println("Captured all methods behavior against nullability of parameter.");
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
}
