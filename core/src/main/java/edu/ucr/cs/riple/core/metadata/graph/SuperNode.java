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

package edu.ucr.cs.riple.core.metadata.graph;

import edu.ucr.cs.riple.core.FixType;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.metadata.method.MethodInheritanceTree;
import edu.ucr.cs.riple.core.metadata.trackers.UsageTracker;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.Fix;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class SuperNode extends AbstractNode {

  public final Set<Fix> followUps;
  public Report report;
  private final Node root;

  public SuperNode(Fix fix) {
    super(fix);
    followUps = new HashSet<>();
    root = new Node(fix);
    this.followUps.add(root.fix);
  }

  @Override
  public void updateUsages(UsageTracker tracker) {
    this.usages.clear();
    followUps.forEach(fix -> usages.addAll(tracker.getUsage(fix)));
  }

  // Here we do not need to subtract referred for method params since we are observing
  // call sites too.
  @Override
  public void setEffect(int effect, MethodInheritanceTree tree, List<Fix> fixes) {
    final int[] total = {effect};
    followUps.forEach(
        fix -> {
          if (fix.location.equals(FixType.PARAMETER.name)) {
            total[0] += Utility.calculateParamInheritanceViolationError(tree, fix);
          }
        });
    this.effect = total[0];
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), followUps, report, root);
  }

  @Override
  public String toString() {
    return "SuperNode{" + "followUps=" + followUps + ", report=" + report + ", root=" + root + '}';
  }

  public Set<Fix> getFixChain() {
    return followUps;
  }

  public void mergeTriggered() {
    this.followUps.addAll(this.triggered);
    this.triggered.clear();
  }
}
