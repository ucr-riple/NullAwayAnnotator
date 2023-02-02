/*
 * MIT License
 *
 * Copyright (c) 2022 anonymous
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

package com.example.tool.core.evaluators.graphprocessor;

import com.example.tool.core.Config;
import com.example.tool.core.evaluators.suppliers.Supplier;
import com.example.tool.core.metadata.graph.ConflictGraph;
import com.example.tool.core.metadata.index.Fix;
import com.example.tool.core.metadata.index.Result;
import com.example.tool.core.util.Utility;
import java.util.Set;
import me.tongfei.progressbar.ProgressBar;

/** Basic processor which processes the impact of each node sequentially. */
public class SequentialConflictGraphProcessor extends AbstractConflictGraphProcessor {

  public SequentialConflictGraphProcessor(Config config, CompilerRunner runner, Supplier supplier) {
    super(config, runner, supplier);
  }

  @Override
  public void process(ConflictGraph graph) {
    int count = (int) graph.getNodes().count();
    System.out.println("Scheduling for: " + count + " runs.");
    ProgressBar pb = Utility.createProgressBar("Processing", count);
    graph
        .getNodes()
        .forEach(
            node -> {
              pb.step();
              Set<Fix> fixes = node.tree;
              injector.injectFixes(fixes);
              compilerRunner.run();
              errorStore.saveState();
              Result errorComparisonResult = errorStore.compare();
              node.effect = errorComparisonResult.size;
              node.updateStatus(
                  errorComparisonResult.size,
                  fixes,
                  getTriggeredFixesFromDownstream(node),
                  errorComparisonResult.dif,
                  methodDeclarationTree);
              injector.removeFixes(fixes);
            });
    pb.close();
  }
}
