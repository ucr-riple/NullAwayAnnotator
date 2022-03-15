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
import edu.ucr.cs.riple.core.FixType;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.metadata.graph.FixGraph;
import edu.ucr.cs.riple.core.metadata.graph.Node;
import edu.ucr.cs.riple.core.metadata.index.Bank;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.FixEntity;
import edu.ucr.cs.riple.core.metadata.index.Result;
import edu.ucr.cs.riple.core.metadata.trackers.Usage;
import edu.ucr.cs.riple.core.metadata.trackers.UsageTracker;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.Fix;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import me.tongfei.progressbar.ProgressBar;

public abstract class AdvancedExplorer extends BasicExplorer {

  final FixGraph<Node> fixGraph;
  protected UsageTracker tracker;
  protected final FixType fixType;

  public AdvancedExplorer(
      Annotator annotator,
      List<Fix> fixes,
      Bank<Error> errorBank,
      Bank<FixEntity> fixBank,
      FixType fixType) {
    super(annotator, errorBank, fixBank);
    this.fixType = fixType;
    this.fixGraph = new FixGraph<>(Node::new);
    fixes.forEach(
        fix -> {
          if (isApplicable(fix)) {
            fixGraph.findOrCreate(fix);
          }
        });
    if (fixGraph.nodes.size() > 0) {
      init();
      explore();
    }
  }

  protected abstract void init();

  protected void explore() {
    HashMap<Integer, Set<Node>> groups = fixGraph.getGroups();
    System.out.println("Scheduling for: " + groups.size() + " builds");
    ProgressBar pb = Utility.createProgressBar("Exploring " + fixType.name, groups.values().size());
    for (Set<Node> nodes : groups.values()) {
      pb.step();
      pb.setExtraMessage("Gathering fixes");
      List<Fix> fixes = nodes.stream().map(node -> node.fix).collect(Collectors.toList());
      pb.setExtraMessage("Applying fixes");
      annotator.apply(fixes);
      pb.setExtraMessage("Building");
      FixSerializationConfig.Builder config =
          new FixSerializationConfig.Builder()
              .setSuggest(true, annotator.depth > 0)
              .setAnnotations(annotator.nullableAnnot, "UNKNOWN")
              .setOutputDirectory(annotator.dir.toString());
      annotator.buildProject(config);
      pb.setExtraMessage("Saving state");
      errorBank.saveState(false, true);
      fixBank.saveState(true, true);
      int index = 0;
      for (Node node : nodes) {
        pb.setExtraMessage("Node number: " + (index++) + " / " + nodes.size());
        int totalEffect = 0;
        for (Usage usage : node.usages) {
          Result<Error> errorComparison =
              errorBank.compareByMethod(usage.clazz, usage.method, false);
          node.analyzeStatus(errorComparison.dif);
          totalEffect += errorComparison.size;
          if (annotator.depth > 0) {
            node.updateTriggered(
                fixBank
                    .compareByMethod(usage.clazz, usage.method, false)
                    .dif
                    .stream()
                    .map(fixEntity -> fixEntity.fix)
                    .collect(Collectors.toList()));
          }
        }
        node.setEffect(totalEffect, annotator.methodInheritanceTree, fixes);
      }
      annotator.remove(fixes);
    }
    pb.close();
  }

  protected Report predict(Fix fix) {
    Node node = fixGraph.find(fix);
    if (node == null) {
      return null;
    }
    Report report = new Report(fix, node.effect);
    report.triggered = node.triggered;
    report.finished = node.finished;
    return report;
  }

  protected abstract Report effectByScope(Fix fix);

  @Override
  public Report effect(Fix fix) {
    Report report = predict(fix);
    if (report != null) {
      return report;
    }
    return effectByScope(fix);
  }

  @Override
  public boolean requiresInjection(Fix fix) {
    return fixGraph.find(fix) == null;
  }
}
