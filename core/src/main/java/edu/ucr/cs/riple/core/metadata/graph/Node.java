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
import edu.ucr.cs.riple.core.metadata.method.MethodInheritanceTree;
import edu.ucr.cs.riple.core.metadata.trackers.RegionTracker;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.Fix;
import java.util.Collections;
import java.util.List;

public class Node extends AbstractNode {

  public Node(Fix fix) {
    super(fix);
  }

  public void updateUsages(RegionTracker tracker) {
    this.regions.addAll(this.rootSource);
    this.regions.addAll(tracker.getRegions(this.fix));
  }

  @Override
  public void setEffect(int localEffect, MethodInheritanceTree tree, List<Fix> fixesInOneRound) {
    // Need to subtract referred in PARAMETER since all errors are happening
    // inside the method boundary and all referred sites are outside (at call sites)
    if (fix.location.equals(FixType.PARAMETER.name)) {
      this.effect =
          localEffect
              - this.fix.referred
              + Utility.calculateParamInheritanceViolationError(
                  tree, this.fix, Collections.emptyList());
    }
    if (fix.location.equals(FixType.METHOD.name)) {
      this.effect =
          localEffect
              + Utility.calculateMethodInheritanceViolationError(tree, this.fix, fixesInOneRound);
    }
    if (fix.location.equals(FixType.FIELD.name)) {
      this.effect = localEffect;
    }
  }

  /**
   * Generates suggested fixes due to making a parameter {@code Nullable} for all overriding
   * methods.
   *
   * @param mit Method Inheritance Tree.
   * @return List of Fixes
   */
  @Override
  public List<Fix> generateSubMethodParameterInheritanceFixes(
      MethodInheritanceTree mit, List<Fix> fixesInOneRound) {
    return generateSubMethodParameterInheritanceFixesByFix(fix, mit);
  }

  @Override
  public String toString() {
    return "Node{"
        + "fix=["
        + fix.method
        + " "
        + fix.className
        + " "
        + fix.variable
        + " "
        + fix.location
        + "]"
        + ", id="
        + id
        + '}';
  }
}
