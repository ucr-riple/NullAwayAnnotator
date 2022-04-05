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
import edu.ucr.cs.riple.core.metadata.method.MethodNode;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.Fix;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
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
    List<Node> allNodes = fixGraph.getAllNodes();
    System.out.println("Scheduled for: " + maxsize + " builds for: " + allNodes.size() + " fixes");
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
        continue;
      }
      FixSerializationConfig.Builder config =
          new FixSerializationConfig.Builder()
              .setSuggest(true, true)
              .setAnnotations(annotator.nullableAnnot, "UNKNOWN")
              .setParamProtectionTest(true, i)
              .setOutputDirectory(annotator.dir.toString());
      annotator.buildProject(config);
      errorBank.saveState(false, true);
      fixBank.saveState(false, true);
      for (Node node : subList) {
        Result<Error> errorComparison =
            errorBank.compareByMethod(node.fix.className, node.fix.method, false);
        node.setEffect(errorComparison.size, annotator.methodInheritanceTree, null);
        node.analyzeStatus(errorComparison.dif);
        if (annotator.depth > 0) {
          List<Fix> triggered = new ArrayList<>();
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

  /**
   * Generates suggested fixes due to making a parameter {@code Nullable} for all overriding methods.
   * @param node Node containing the fix.
   * @return List of Fixes
   */
  private List<Fix> generateSubMethodParameterInheritanceFixes(Node node){
    List<MethodNode> overridingMethods = annotator.methodInheritanceTree.getSubMethods(node.fix.method, node.fix.className, false);
    int index = Integer.parseInt(node.fix.index);
    Fix rootFix = node.fix;
    List<Fix> ans = new ArrayList<>();
    overridingMethods.stream().forEach(new Consumer<MethodNode>() {
      @Override
      public void accept(MethodNode methodNode) {
        if(index < methodNode.annotFlags.length && !methodNode.annotFlags[index]){
          Fix fix = rootFix.duplicate();
          // make fix here...
//          fix.
//          ans.add()
        }
      }
    });
  }

  @Override
  public boolean isApplicable(Fix fix) {
    return fix.location.equals(fixType.name);
  }
}
