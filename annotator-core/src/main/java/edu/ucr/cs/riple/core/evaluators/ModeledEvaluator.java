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
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.model.DynamicModel;
import edu.ucr.cs.riple.core.model.Impact;
import java.util.Set;
import java.util.stream.Collectors;

/** This evaluator uses modeling techniques to compute effect of applying a fix tree. */
public class ModeledEvaluator extends AbstractEvaluator {

  /** Model to retrieve impacts. */
  private final DynamicModel<Impact> model;

  public ModeledEvaluator(Supplier supplier) {
    super(supplier);
    this.model = new DynamicModel<>(supplier.getConfig(), supplier.getMethodDeclarationTree());
  }

  @Override
  protected void collectGraphResults(ImmutableSet<Report> reports) {
    // update model with new data.
    model.updateModelStore(
        graph
            .getNodes()
            .map(node -> new Impact(node.root, node.triggeredErrors))
            .collect(Collectors.toSet()));
    // compute effects.
    reports.forEach(
        report -> {
          Fix root = report.root;
          Set<Error> triggeredErrors = model.getTriggeredErrorsForCollection(report.tree);
          report.localEffect = triggeredErrors.size() - root.count;
        });
  }

  @Override
  protected void initializeFixGraph(ImmutableSet<Report> reports) {
    super.initializeFixGraph(reports);
    // add only fixes that are not stored in model.
    reports.stream()
        .filter(report -> !report.isInProgress(config))
        .flatMap(report -> report.tree.stream())
        .filter(model::isUnknown)
        .forEach(graph::addNodeToVertices);
  }
}
