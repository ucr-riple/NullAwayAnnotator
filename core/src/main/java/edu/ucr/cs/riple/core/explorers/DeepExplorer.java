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

import com.uber.nullaway.fixserialization.FixSerializationConfig;
import edu.ucr.cs.riple.core.Annotator;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.metadata.graph.FixGraph;
import edu.ucr.cs.riple.core.metadata.graph.SuperNode;
import edu.ucr.cs.riple.core.metadata.index.Bank;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.FixEntity;
import edu.ucr.cs.riple.core.metadata.index.Result;
import edu.ucr.cs.riple.core.metadata.trackers.CompoundTracker;
import edu.ucr.cs.riple.core.metadata.trackers.Region;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.Fix;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import me.tongfei.progressbar.ProgressBar;

public class DeepExplorer extends BasicExplorer {

  private final CompoundTracker tracker;
  private final FixGraph<SuperNode> fixGraph;

  public DeepExplorer(Annotator annotator, Bank<Error> errorBank, Bank<FixEntity> fixBank) {
    super(annotator, errorBank, fixBank);
    this.tracker = new CompoundTracker(annotator.fieldRegionTracker, annotator.methodRegionTracker);
    this.fixGraph = new FixGraph<>(SuperNode::new);
  }

  private boolean init(List<Report> reports) {
    this.fixGraph.clear();
    Set<Report> filteredReports =
        reports.stream().filter(report -> !report.finished).collect(Collectors.toSet());
    filteredReports.forEach(
        report -> {
          Fix fix = report.fix;
          SuperNode node = fixGraph.findOrCreate(fix);
          node.report = report;
          node.triggered = report.triggered;
          node.tree.addAll(report.followups);
          node.mergeTriggered();
          node.updateUsages(tracker);
          node.changed = false;
        });
    return filteredReports.size() > 0;
  }

  public void start(List<Report> reports) {
    if (annotator.depth == 0) {
      reports.forEach(report -> report.finished = true);
      return;
    }
    System.out.println("Deep explorer is active...\nMax Depth level: " + annotator.depth);
    for (int i = 0; i < annotator.depth; i++) {
      System.out.print("Analyzing at level " + (i + 1) + ", ");
      if (!init(reports)) {
        break;
      }
      explore();
      List<SuperNode> nodes = fixGraph.getAllNodes();
      nodes.forEach(
          superNode -> {
            Report report = superNode.report;
            report.effectiveNess = superNode.effect;
            report.followups = superNode.tree;
            report.triggered = superNode.triggered;
            report.finished = !superNode.changed;
          });
    }
  }

  private void explore() {
    if (fixGraph.nodes.size() == 0) {
      return;
    }
    fixGraph.findGroups();
    HashMap<Integer, Set<SuperNode>> groups = fixGraph.getGroups();
    System.out.println(
        "Scheduling for: "
            + groups.size()
            + " builds for: "
            + fixGraph.getAllNodes().size()
            + " reports");
    ProgressBar pb = Utility.createProgressBar("Deep analysis", groups.size());
    pb.setExtraMessage("Building");
    for (Set<SuperNode> group : groups.values()) {
      pb.step();
      List<Fix> fixes = new ArrayList<>();
      group.forEach(superNode -> fixes.addAll(superNode.getFixChain()));
      annotator.apply(fixes);
      FixSerializationConfig.Builder config =
          new FixSerializationConfig.Builder()
              .setSuggest(true, true)
              .setAnnotations(annotator.nullableAnnot, "UNKNOWN")
              .setOutputDirectory(annotator.dir.toString());
      annotator.buildProject(config);
      errorBank.saveState(false, true);
      fixBank.saveState(false, true);
      group.forEach(
          superNode -> {
            int totalEffect = 0;
            List<Fix> localTriggered = new ArrayList<>();
            for (Region region : superNode.regions) {
              Result<Error> res = errorBank.compareByMethod(region.clazz, region.method, false);
              totalEffect += res.size;
              localTriggered.addAll(
                  fixBank
                      .compareByMethod(region.clazz, region.method, false)
                      .dif
                      .stream()
                      .map(fixEntity -> fixEntity.fix)
                      .collect(Collectors.toList()));
            }
            localTriggered.addAll(
                superNode.generateSubMethodParameterInheritanceFixes(
                    annotator.methodInheritanceTree, fixes));
            superNode.updateTriggered(localTriggered);
            superNode.setEffect(totalEffect, annotator.methodInheritanceTree, fixes);
          });
      annotator.remove(fixes);
    }
    pb.close();
  }
}
