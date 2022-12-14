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
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.evaluators.graphprocessor.ConflictGraphProcessor;
import edu.ucr.cs.riple.core.evaluators.suppliers.Supplier;
import edu.ucr.cs.riple.core.metadata.graph.ConflictGraph;
import edu.ucr.cs.riple.core.metadata.index.Fix;

/** Abstract class for evaluators. */
public abstract class AbstractEvaluator implements Evaluator {

  /** Annotator config. */
  protected final Config config;
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
    this.config = supplier.getConfig();
    this.graph = new ConflictGraph();
    this.processor = supplier.getGraphProcessor();
  }

  /**
   * Initializes conflict graph for the upcoming iteration.
   *
   * @param reports The latest created reports from the fixes.
   */
  protected void initializeFixGraph(ImmutableSet<Report> reports) {
    this.graph.clear();
  }

  /** Collects results created by the processors working on the conflict graph. */
  protected abstract void collectGraphResults(ImmutableSet<Report> reports);

  @Override
  public ImmutableSet<Report> evaluate(ImmutableSet<Fix> fixes) {
    ImmutableSet<Report> reports =
        fixes.stream().map(fix -> new Report(fix, 1)).collect(ImmutableSet.toImmutableSet());
    System.out.println("Max Depth level: " + this.depth);
    for (int i = 0; i < this.depth; i++) {
      System.out.print("Analyzing at level " + (i + 1) + ", ");
      initializeFixGraph(reports);
      config.log.updateNodeNumber(graph.getNodes().count());
      if (!graph.isEmpty()) {
        processor.process(graph);
      }
      collectGraphResults(reports);
    }
    return reports;
  }
}
