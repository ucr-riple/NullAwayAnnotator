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
import edu.ucr.cs.riple.core.metadata.index.Bank;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.FixEntity;
import edu.ucr.cs.riple.core.metadata.method.MethodInheritanceTree;
import edu.ucr.cs.riple.core.metadata.method.MethodNode;
import edu.ucr.cs.riple.core.metadata.trackers.Region;
import edu.ucr.cs.riple.core.metadata.trackers.RegionTracker;
import edu.ucr.cs.riple.injector.Fix;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public abstract class AbstractNode {

  /** Fix to process */
  public final Fix fix;

  public final Set<Region> regions;
  public Set<Fix> triggered;
  public int id;
  /** Effect of applying containing fix */
  public int effect;
  /** if <code>true</code>, set of triggered has been updated */
  public boolean changed;
  /** if <code>true</code>, no new triggered error is addressable by a fix */
  public boolean finished;

  /** Regions where error reported * */
  protected Set<Region> rootSource;

  protected AbstractNode(Fix fix) {
    this.regions = new HashSet<>();
    this.fix = fix;
    this.triggered = new HashSet<>();
    this.effect = 0;
    this.finished = false;
  }

  public void setRootSource(Bank<FixEntity> fixBank) {
    this.rootSource =
        fixBank.getAllSources(
            o -> {
              if (o.fix.equals(this.fix)) {
                return 0;
              }
              return -10;
            });
  }

  public abstract void updateUsages(RegionTracker tracker);

  public boolean hasConflictInUsage(AbstractNode other) {
    return !Collections.disjoint(other.regions, this.regions);
  }

  public abstract void setEffect(
      int localEffect, MethodInheritanceTree tree, List<Fix> fixesInjectedInOneRound);

  public void updateTriggered(List<Fix> fixes) {
    int sizeBefore = this.triggered.size();
    this.triggered.addAll(fixes);
    int sizeAfter = this.triggered.size();
    for (Fix fix : fixes) {
      for (Fix other : this.triggered) {
        if (fix.equals(other)) {
          other.referred++;
          break;
        }
      }
    }
    changed = (changed || (sizeAfter != sizeBefore));
  }

  public void analyzeStatus(List<Error> newErrors) {
    this.finished = newErrors.stream().noneMatch(Error::isFixable);
  }

  @Override
  public int hashCode() {
    return getHash(fix);
  }

  public static int getHash(Fix fix) {
    return Objects.hash(fix.variable, fix.index, fix.className, fix.method);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AbstractNode)) return false;
    AbstractNode node = (AbstractNode) o;
    return fix.equals(node.fix);
  }

  /**
   * Generates suggested fixes due to making a parameter {@code Nullable} for all overriding *
   * methods.
   *
   * @param fix Fix containing the fix parameter nullable suggestion fix.
   * @param mit Method Inheritance Tree.
   * @return List of Fixes
   */
  protected static List<Fix> generateSubMethodParameterInheritanceFixesByFix(
      Fix fix, MethodInheritanceTree mit) {
    List<MethodNode> overridingMethods = mit.getSubMethods(fix.method, fix.className, false);
    int index = Integer.parseInt(fix.index);
    List<Fix> ans = new ArrayList<>();
    overridingMethods.forEach(
        methodNode -> {
          if (index < methodNode.annotFlags.length && !methodNode.annotFlags[index]) {
            Fix newFix =
                new Fix(
                    fix.annotation,
                    fix.method,
                    methodNode.parameterNames[index],
                    FixType.PARAMETER.name,
                    methodNode.clazz,
                    methodNode.uri,
                    "true");
            newFix.index = String.valueOf(index);
            ans.add(newFix);
          }
        });
    return ans;
  }

  /**
   * Generates suggested fixes due to making a parameter {@code Nullable} for all overriding
   * methods.
   *
   * @param mit Method Inheritance Tree.
   * @return List of Fixes
   */
  public abstract List<Fix> generateSubMethodParameterInheritanceFixes(
      MethodInheritanceTree mit, List<Fix> fixesInOneRound);
}
