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

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.explorers.suppliers.Supplier;
import edu.ucr.cs.riple.core.global.GlobalAnalyzer;
import edu.ucr.cs.riple.core.metadata.graph.Node;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.index.Result;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.changes.AddAnnotation;
import edu.ucr.cs.riple.injector.location.Location;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import me.tongfei.progressbar.ProgressBar;

public class BasicExplorer extends Explorer {

  public BasicExplorer(ImmutableSet<Fix> fixes, Supplier supplier, GlobalAnalyzer globalAnalyzer) {
    super(fixes, supplier, globalAnalyzer);
  }

  @Override
  protected void executeNextCycle() {
    System.out.println(
        "Scheduling for: " + reports.size() + " builds for: " + reports.size() + " fixes");
    ProgressBar pb = Utility.createProgressBar("Processing", reports.size());
    graph
        .getNodes()
        .forEach(
            node -> {
              pb.step();
              Set<Fix> fixes = node.tree;
              injector.injectFixes(fixes);
              Utility.buildTarget(config);
              errorBank.saveState(false, true);
              fixBank.saveState(false, true);
              Result<Error> errorComparisonResult = errorBank.compare();
              node.effect = errorComparisonResult.size;
              Collection<Fix> fixComparisonResultDif = fixBank.compare().dif;
              addTriggeredFixesFromDownstream(node, fixComparisonResultDif);
              node.updateStatus(
                  errorComparisonResult.size,
                  fixes,
                  fixComparisonResultDif,
                  errorComparisonResult.dif,
                  methodDeclarationTree);
              injector.removeFixes(fixes);
            });
    pb.close();
  }

  /**
   * Updates list of triggered fixes with fixes triggered from downstream dependencies.
   *
   * @param node Node in process.
   * @param localTriggeredFixes Collection of triggered fixes locally.
   */
  public void addTriggeredFixesFromDownstream(Node node, Collection<Fix> localTriggeredFixes) {
    Set<Location> currentLocationTargetedByTree =
        node.tree.stream().map(fix -> fix.change.location).collect(Collectors.toSet());
    localTriggeredFixes.addAll(
        globalAnalyzer.getImpactedParameters(node.tree).stream()
            .filter(input -> !currentLocationTargetedByTree.contains(input))
            .map(
                onParameter ->
                    new Fix(
                        new AddAnnotation(onParameter, config.nullableAnnot),
                        "PASSING_NULLABLE",
                        onParameter.clazz,
                        onParameter.method,
                        false))
            .collect(Collectors.toList()));
  }
}
