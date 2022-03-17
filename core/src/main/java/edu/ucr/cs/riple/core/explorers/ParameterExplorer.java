/*
 * MIT License
 *
 * Copyright (c) 2020 Nima Karimipour
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package edu.ucr.cs.riple.core.explorers;

import com.uber.nullaway.fixserialization.FixSerializationConfig;
import edu.ucr.cs.riple.core.Annotator;
import edu.ucr.cs.riple.core.FixType;
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

public class ParameterExplorer extends AdvancedExplorer {

  public ParameterExplorer(
      Annotator annotator, List<Fix> fixes, Bank<Error> errorBank, Bank<FixEntity> fixBank) {
    super(annotator, fixes, errorBank, fixBank, FixType.PARAMETER);
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
              .setSuggest(true, true)
              .setAnnotations(annotator.nullableAnnot, "UNKNOWN")
              .setParamProtectionTest(true, i)
              .setOutputDirectory(annotator.dir.toString());
      annotator.buildProject(config);
      errorBank.saveState(false, true);
      fixBank.saveState(false, true);
      int index = 0;
      for (Node node : subList) {
        pb.setExtraMessage("processing node: " + index + " / " + subList.size());
        Result<Error> errorComparison =
            errorBank.compareByMethod(node.fix.className, node.fix.method, false);
        node.setEffect(errorComparison.size, annotator.methodInheritanceTree, null);
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
    System.out.println("Captured all methods behavior against nullability of parameters.");
  }

  @Override
  public boolean isApplicable(Fix fix) {
    return fix.location.equals(fixType.name);
  }
}
