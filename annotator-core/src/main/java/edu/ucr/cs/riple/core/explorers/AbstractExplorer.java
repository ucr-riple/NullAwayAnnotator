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
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.explorers.impactanalyzers.ImpactAnalyzer;
import edu.ucr.cs.riple.core.explorers.suppliers.Supplier;
import edu.ucr.cs.riple.core.metadata.graph.ConflictGraph;
import edu.ucr.cs.riple.core.metadata.index.Fix;

public abstract class AbstractExplorer implements Explorer {

  protected final ImmutableSet<Report> reports;
  protected final Config config;
  protected final ConflictGraph graph;
  protected final int depth;
  protected ImpactAnalyzer analyzer;
  protected final Supplier supplier;

  public AbstractExplorer(ImmutableSet<Fix> fixes, Supplier supplier) {
    this.supplier = supplier;
    this.reports =
        fixes.stream().map(fix -> new Report(fix, 1)).collect(ImmutableSet.toImmutableSet());
    this.depth = supplier.depth();
    this.config = supplier.getConfig();
    this.graph = new ConflictGraph();
    this.analyzer = supplier.getImpactAnalyzer();
  }

  protected void initializeFixGraph() {
    this.graph.clear();
  }

  protected abstract void collectGraphResults();

  @Override
  public ImmutableSet<Report> explore() {
    System.out.println("Max Depth level: " + config.depth);
    for (int i = 0; i < this.depth; i++) {
      System.out.print("Analyzing at level " + (i + 1) + ", ");
      initializeFixGraph();
      config.log.updateNodeNumber(graph.getNodes().count());
      if (graph.isEmpty()) {
        System.out.println("Analysis finished at this iteration.");
        break;
      }
      analyzer.analyzeImpacts(graph);
      collectGraphResults();
    }
    return reports;
  }
}
