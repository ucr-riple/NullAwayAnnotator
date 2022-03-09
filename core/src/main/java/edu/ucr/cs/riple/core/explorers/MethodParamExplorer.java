package edu.ucr.cs.riple.core.explorers;

import com.uber.nullaway.fixserialization.FixSerializationConfig;
import edu.ucr.cs.riple.core.Annotator;
import edu.ucr.cs.riple.core.FixType;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.metadata.graph.Node;
import edu.ucr.cs.riple.core.metadata.index.Bank;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.FixEntity;
import edu.ucr.cs.riple.core.metadata.index.Result;
import edu.ucr.cs.riple.core.metadata.method.MethodInheritanceTree;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.Fix;
import java.util.List;
import java.util.stream.Collectors;
import me.tongfei.progressbar.ProgressBar;

public class MethodParamExplorer extends AdvancedExplorer {

  public MethodParamExplorer(
      Annotator annotator, List<Fix> fixes, Bank<Error> errorBank, Bank<FixEntity> fixBank) {
    super(annotator, fixes, errorBank, fixBank, FixType.METHOD_PARAM);
  }

  @Override
  protected void init() {}

  @Override
  protected void explore() {
    int maxsize = MethodInheritanceTree.maxParamSize();
    System.out.println("Max size for method parameter list is: " + maxsize);
    List<Node> allNodes = fixGraph.getAllNodes();
    ProgressBar pb = Utility.createProgressBar("Exploring Method Params: ", maxsize);
    for (int i = 0; i < maxsize; i++) {
      pb.step();
      pb.setExtraMessage(
          "Analyzing params at index: (" + (i + 1) + " out of " + maxsize + ") for all methods...");
      int finalI1 = i;
      List<Node> subList =
          allNodes
              .stream()
              .filter(node -> node.fix.index.equals(finalI1 + ""))
              .collect(Collectors.toList());
      if (subList.size() == 0) {
        pb.setExtraMessage("No fix at this index, skipping.");
        continue;
      }
      pb.setExtraMessage("Building.");
      FixSerializationConfig.Builder config =
          new FixSerializationConfig.Builder()
              .setSuggest(true, false)
              .setAnnotations(annotator.nullableAnnot, "UNKNOWN")
              .setParamProtectionTest(true, i)
              .setOutputDirectory(annotator.dir.toString());
      ;
      annotator.buildProject(config);
      errorBank.saveState(false, true);
      fixBank.saveState(false, true);
      int index = 0;
      for (Node node : subList) {
        pb.setExtraMessage("processing node: " + index + " / " + subList.size());
        Result<Error> errorComparison =
            errorBank.compareByMethod(node.fix.className, node.fix.method, false);
        node.setEffect(errorComparison.size, annotator.methodInheritanceTree);
        node.analyzeStatus(errorComparison.dif);
        if (annotator.depth > 0) {
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
    pb.close();
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
}
