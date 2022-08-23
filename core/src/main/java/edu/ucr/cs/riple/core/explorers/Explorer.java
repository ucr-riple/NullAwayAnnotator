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
import edu.ucr.cs.riple.core.injectors.AnnotationInjector;
import edu.ucr.cs.riple.core.metadata.graph.ConflictGraph;
import edu.ucr.cs.riple.core.metadata.graph.Node;
import edu.ucr.cs.riple.core.metadata.index.Bank;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.method.MethodInheritanceTree;

public abstract class Explorer {
  protected final AnnotationInjector injector;
  protected final Bank<Error> errorBank;
  protected final Bank<Fix> fixBank;
  protected final ImmutableSet<Report> reports;
  protected final Config config;
  protected final ConflictGraph graph;

  protected final MethodInheritanceTree methodInheritanceTree;

  protected final int depth;

  public Explorer(
      AnnotationInjector injector,
      Bank<Error> errorBank,
      Bank<Fix> fixBank,
      ImmutableSet<Fix> fixes,
      MethodInheritanceTree methodInheritanceTree,
      int depth,
      Config config) {
    this.injector = injector;
    this.errorBank = errorBank;
    this.fixBank = fixBank;
    this.methodInheritanceTree = methodInheritanceTree;
    this.reports =
        fixes.stream().map(fix -> new Report(fix, 1)).collect(ImmutableSet.toImmutableSet());
    this.config = config;
    this.depth = depth;
    this.graph = new ConflictGraph();
  }

  protected void initializeFixGraph() {
    this.graph.clear();
    this.reports.stream()
        .filter(report -> !report.finished && (!config.bailout || report.localEffect > 0))
        .forEach(
            report -> {
              Fix root = report.root;
              Node node = graph.addNodeToVertices(root);
              node.setOrigins(fixBank);
              node.report = report;
              node.triggered = report.triggered;
              node.tree.addAll(report.tree);
              node.mergeTriggered();
            });
  }

  protected void finalizeReports() {
    graph
        .getNodes()
        .forEach(
            node -> {
              Report report = node.report;
              report.localEffect = node.effect;
              report.tree = node.tree;
              report.triggered = node.triggered;
              report.finished = !node.changed;
            });
  }

  protected abstract void executeNextCycle();

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
      executeNextCycle();
      finalizeReports();
    }
    return reports;
  }
}
