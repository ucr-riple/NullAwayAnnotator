/*
 * Copyright (c) 2022 University of California, Riverside.
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
import edu.ucr.cs.riple.core.metadata.method.MethodInheritanceTree;
import edu.ucr.cs.riple.core.metadata.trackers.UsageTracker;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.Fix;

public class Node extends AbstractNode {

  public Node(Fix fix) {
    super(fix);
  }

  public void updateUsages(UsageTracker tracker) {
    this.usages.addAll(tracker.getUsage(this.fix));
  }

  // We need to subtract referred in METHOD_PARAM since all errors are happening
  // inside the method boundary and all referred sites are outside (at call sites)
  @Override
  public void setEffect(int localEffect, MethodInheritanceTree tree) {
    if (fix.location.equals(FixType.PARAMETER.name)) {
      this.effect =
          localEffect
              - this.fix.referred
              + Utility.calculateInheritanceViolationError(tree, this.fix);
    } else {
      this.effect = localEffect;
    }
  }

  @Override
  public String toString() {
    return "Node{"
        + "fix=["
        + fix.method
        + " "
        + fix.className
        + " "
        + fix.param
        + " "
        + fix.location
        + "]"
        + ", id="
        + id
        + ", effect="
        + effect
        + ", referred="
        + fix.referred
        + '}';
  }
}
