/*
 * MIT License
 *
 * Copyright (c) 2023 Nima Karimipour
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

/**
 * This evaluator uses caching techniques to optimize computing the effect of applying a fix tree.
 * To prepare the conflict graph, each report that requires further investigation is selected and
 * only fixes that are not stored in the cache will be selected. Each selected fix will be processed
 * individually. Once the impact of each node is computed, all processed nodes will be added to the
 * using cache for later retrievals and their corresponding reports are created.
 */
public class CachedEvaluator extends AbstractEvaluator {

  /** Cache to retrieve impacts. */
  private final TargetModuleCache cache;

  public CachedEvaluator(Supplier supplier) {
    super(supplier);
    this.cache = supplier.getTargetModuleCache();
  }

  /**
   * Prepares the conflict graph by selecting reports that are not finalized, and then only filing
   * conflict graph with fixes that are not stored in the cache. Each node in the prepared conflict
   * graph contains a single fix (this fix is unique across all nodes) that is included in one more
   * fix trees from reports.
   *
   * @param reports The latest created reports from previous iteration.
   */
  @Override
  protected void initializeFixGraph(ImmutableSet<Report> reports) {
    super.initializeFixGraph(reports);
    // add only fixes that are not stored in cache.
    Set<Fix> fixes =
        reports.stream()
            .filter(report -> report.requiresFurtherProcess(context.config))
            .flatMap(report -> report.getFixesForNextIteration().stream())
            .filter(cache::isUnknown)
            .collect(Collectors.toSet());
    fixes.forEach(graph::addNodeToVertices);
    System.out.println(
        "Retrieved "
            + (reports.stream().mapToLong(r -> r.tree.size()).sum() - graph.getNodes().count())
            + "/"
            + cache.size()
            + " impact(s) from cache...");
  }

  /**
   * Collects results from the conflict graph to update cache and construct reports. First it
   * updates cache with the new data containing the impact of fixes that has been processed. Then it
   * collects the set of fixes each report requested to be processed in the conflict graph, and use
   * those fixes impacts to construct the new tree (including the triggered fixes) and set of
   * triggered errors to create the corresponding reports.
   *
   * @param reports The latest created reports from the fixes.
   */
  @Override
  protected void collectGraphResults(ImmutableSet<Report> reports) {
    // update cache with new data.
    cache.updateCacheState(
        graph
            .getNodes()
            .map(
                node ->
                    new Impact(
                        node.root, node.triggeredErrors, node.triggeredFixesFromDownstreamErrors))
            .collect(Collectors.toSet()));

    // collect requested fixes for each report which was added to conflict graph.
    Map<Report, Set<Fix>> reportFixMap =
        reports.stream()
            .filter(report -> report.requiresFurtherProcess(context.config))
            .collect(toMap(identity(), Report::getFixesForNextIteration));

    // update reports state.
    reportFixMap.forEach(
        (report, processedFixes) -> {
          // update the tree with the new triggered fixes.
          Set<Fix> newTree = Sets.newHashSet(report.tree);
          newTree.addAll(processedFixes);
          // compute the set of triggered errors for the entire tree.
          Set<Error> triggeredErrors = cache.getTriggeredErrorsForCollection(newTree);
          report.localEffect =
              triggeredErrors.size()
                  - supplier
                      .getErrorStore()
                      .getNumberOfErrorsResolvedByAllFixesWithinCollection(newTree);
          report.triggeredErrors = ImmutableSet.copyOf(triggeredErrors);
          // get fixes triggered from downstream.
          report.triggeredFixesFromDownstreamErrors =
              cache.getTriggeredFixesOnDownstreamForCollection(newTree);
          // replace the old tree with new tree that contains triggered fixes from this iteration.
          report.tree = newTree;
          report.hasBeenProcessedOnce = true;
        });
  }
}
