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

import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.toMap;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.cache.Impact;
import edu.ucr.cs.riple.core.cache.TargetModuleCache;
import edu.ucr.cs.riple.core.evaluators.suppliers.Supplier;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** This evaluator uses modeling techniques to compute effect of applying a fix tree. */
public class CachedEvaluator extends AbstractEvaluator {

  /** Model to retrieve impacts. */
  private final TargetModuleCache<Impact> cache;

  public CachedEvaluator(Supplier supplier) {
    super(supplier);
    this.cache = new TargetModuleCache<>(supplier.getConfig(), supplier.getMethodDeclarationTree());
  }

  @Override
  protected void initializeFixGraph(ImmutableSet<Report> reports) {
    super.initializeFixGraph(reports);
    // add only fixes that are not stored in model.
    reports.stream()
        .filter(report -> report.isInProgress(config))
        .flatMap(report -> report.getFixesForNextIteration().stream())
        .filter(cache::isUnknown)
        .forEach(graph::addNodeToVertices);
  }

  @Override
  protected void collectGraphResults(ImmutableSet<Report> reports) {
    // update cache with new data.
    cache.updateCacheState(
        graph
            .getNodes()
            .map(
                node ->
                    new Impact(node.root, node.triggeredErrors, node.triggeredFixesOnDownstream))
            .collect(Collectors.toSet()));

    // collect requested fixes for each report.
    Map<Report, Set<Fix>> reportFixMap =
        reports.stream().collect(toMap(identity(), Report::getFixesForNextIteration));

    // update reports state.
    reportFixMap.forEach(
        (report, processedFixes) -> {
          Set<Fix> newTree = Sets.newHashSet(report.tree);
          newTree.addAll(processedFixes);
          Set<Error> triggeredErrors = cache.getTriggeredErrorsForCollection(newTree);
          report.localEffect =
              triggeredErrors.size()
                  - supplier.getErrorStore().getNumberOfResolvedFixesWithCollection(newTree);
          report.triggeredErrors = ImmutableSet.copyOf(triggeredErrors);
          report.triggeredFixesOnDownstream =
              cache.getTriggeredFixesOnDownstreamForCollection(newTree);
          report.tree = newTree;
          report.opened = true;
        });
  }
}
