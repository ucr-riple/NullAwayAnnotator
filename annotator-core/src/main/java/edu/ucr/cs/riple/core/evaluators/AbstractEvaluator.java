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
import edu.ucr.cs.riple.core.Context;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.evaluators.graph.ConflictGraph;
import edu.ucr.cs.riple.core.evaluators.graph.processors.ConflictGraphProcessor;
import edu.ucr.cs.riple.core.evaluators.suppliers.Supplier;
import edu.ucr.cs.riple.core.metadata.index.Fix;

/**
 * Abstract class for evaluators. Subclasses of this evaluator, computes the effectiveness of fix
 * trees by inserting annotations to the source code and then rerunning the analysis as an oracle to
 * computes the effectiveness based on the number of triggered errors. The investigation process, is
 * an iterative process where at each iteration, the fix tree is analyzed at the next depth. At each
 * iteration, set of fixes (each node in conflict graph can contain a single or a group of fixes)
 * that requires to be injected are collected by {@link AbstractEvaluator#initializeFixGraph} and
 * fills the conflict graph. The resulting conflict graph is processed by the using {@link
 * AbstractEvaluator#processor} which computes the set of triggered errors for each node. Finally
 * {@link AbstractEvaluator#collectGraphResults} is called which reads the result from the graph and
 * creates the corresponding reports.
 */
public abstract class AbstractEvaluator implements Evaluator {

  /** Annotator context. */
  protected final Context context;
  /** Conflict graph to storing unprocessed fixes. */
  protected final ConflictGraph graph;
  /** Depth of analysis. */
  protected final int depth;
  /** Graph processor to process the graph. */
  protected ConflictGraphProcessor processor;
  /** Supplier used for initialization. */
  protected final Supplier supplier;

  public AbstractEvaluator(Supplier supplier) {
    this.supplier = supplier;
    this.depth = supplier.depth();
    this.context = supplier.getContext();
    this.graph = new ConflictGraph();
    this.processor = supplier.getGraphProcessor();
  }

  /**
   * Initializes conflict graph for the upcoming iteration.
   *
   * @param reports The latest created reports from previous iteration.
   */
  protected void initializeFixGraph(ImmutableSet<Report> reports) {
    this.graph.clear();
  }

  /**
   * Collects results created by the processors working on the conflict graph.
   *
   * @param reports The latest created reports from previous iteration.
   */
  protected abstract void collectGraphResults(ImmutableSet<Report> reports);

  @Override
  public ImmutableSet<Report> evaluate(ImmutableSet<ImmutableSet<Fix>> fixes) {
    ImmutableSet<Report> reports =
        fixes.stream()
            .map(fix -> new Report(fix, 1))
            .peek(
                report ->
                    report.reflectAnnotationProcessorChangesOnSourceCode(supplier.getModuleInfo()))
            .collect(ImmutableSet.toImmutableSet());
    for (int i = 0; i < this.depth; i++) {
      initializeFixGraph(reports);
      context.log.updateNodeNumber(graph.getNodes().count());
      if (!graph.isEmpty()) {
        System.out.print("Analyzing at level " + (i + 1) + ", ");
        processor.process(graph);
      }
      collectGraphResults(reports);
    }
    return reports;
  }
}
