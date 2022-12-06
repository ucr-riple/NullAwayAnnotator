/*
 * MIT License
 *
 * Copyright (c) 2022 Nima Karimipour
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

package edu.ucr.cs.riple.core.evaluators;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.evaluators.suppliers.Supplier;
import edu.ucr.cs.riple.core.metadata.graph.Node;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import java.util.HashSet;

/** Basic evaluator that processes each fix tree entirely with no caching strategies. */
public class BasicEvaluator extends AbstractEvaluator {

  public BasicEvaluator(Supplier supplier) {
    super(supplier);
  }

  @Override
  protected void initializeFixGraph(ImmutableSet<Report> reports) {
    super.initializeFixGraph(reports);
    reports.stream()
        .filter(input -> input.isInProgress(config))
        .forEach(
            report -> {
              Fix root = report.root;
              Node node = graph.addNodeToVertices(root);
              node.setOrigins(supplier.getErrorBank());
              node.report = report;
              node.triggeredFixes = new HashSet<>(report.triggeredFixes);
              node.tree.addAll(report.tree);
              node.mergeTriggered();
            });
  }

  @Override
  protected void collectGraphResults(ImmutableSet<Report> reports) {
    graph
        .getNodes()
        .forEach(
            node -> {
              Report report = node.report;
              report.localEffect = node.effect;
              report.tree = node.tree;
              report.triggeredFixes = ImmutableSet.copyOf(node.triggeredFixes);
              report.triggeredErrors = node.triggeredErrors;
              report.finished = !node.changed;
            });
  }
}
