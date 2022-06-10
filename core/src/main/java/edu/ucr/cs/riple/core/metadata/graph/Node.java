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

import com.google.common.collect.Sets;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.metadata.index.Bank;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.method.MethodInheritanceTree;
import edu.ucr.cs.riple.core.metadata.trackers.Region;
import edu.ucr.cs.riple.core.metadata.trackers.RegionTracker;
import edu.ucr.cs.riple.injector.location.OnMethod;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Node {

  /** Fix to process */
  public final Fix root;

  /** Tree of all fixes connecting to root. */
  public final Set<Fix> tree;

  public final Set<Region> regions;

  public Set<Fix> triggered;

  public int id;

  /** Effect of applying containing change */
  public int effect;

  /** if <code>true</code>, set of triggered has been updated */
  public boolean changed;

  public Report report;

  /** Regions where original errors reported, all are for root node */
  private Set<Region> rootSource;

  public Node(Fix root) {
    this.regions = new HashSet<>();
    this.root = root;
    this.triggered = new HashSet<>();
    this.effect = 0;
    this.tree = Sets.newHashSet(root);
  }

  public void setRootSource(Bank<Fix> fixBank) {
    this.rootSource = fixBank.getRegionsForFixes(fix -> fix.equals(root));
  }

  public void updateRegions(RegionTracker tracker) {
    this.regions.clear();
    this.regions.addAll(this.rootSource);
    tree.forEach(fix -> regions.addAll(tracker.getRegions(fix)));
    tree.stream()
        .filter(fix -> fix.isOnParameter() && fix.isModifyingConstructor())
        .forEach(fix -> regions.add(new Region("null", fix.change.location.clazz)));
  }

  public boolean hasConflictInRegions(Node other) {
    return !Collections.disjoint(other.regions, this.regions);
  }

  public void updateStatus(
      int effect, Set<Fix> fixesInOneRound, List<Fix> triggered, MethodInheritanceTree mit) {
    updateTriggered(triggered);
    final int[] numberOfSuperMethodsAnnotatedOutsideTree = {0};
    this.tree.stream()
        .filter(Fix::isOnMethod)
        .map(
            fix -> {
              OnMethod onMethod = fix.toMethod();
              return mit.getClosestSuperMethod(onMethod.method, onMethod.clazz);
            })
        .filter(node -> node != null && !node.hasNullableAnnotation)
        .forEach(
            node -> {
              if (tree.stream()
                  .anyMatch(
                      fix ->
                          fix.isOnMethod()
                              && fix.toMethod().method.equals(node.method)
                              && fix.toMethod().clazz.equals(node.clazz))) {
                return;
              }
              if (fixesInOneRound.stream()
                  .anyMatch(
                      fix ->
                          fix.isOnMethod()
                              && fix.toMethod().method.equals(node.method)
                              && fix.toMethod().clazz.equals(node.clazz))) {
                numberOfSuperMethodsAnnotatedOutsideTree[0]++;
              }
            });
    this.effect = effect + numberOfSuperMethodsAnnotatedOutsideTree[0];
  }

  public void updateTriggered(List<Fix> fixes) {
    int sizeBefore = this.triggered.size();
    this.triggered.addAll(fixes);
    int sizeAfter = this.triggered.size();
    changed = (changed || (sizeAfter != sizeBefore));
  }

  public void mergeTriggered() {
    this.tree.addAll(this.triggered);
    this.triggered.clear();
  }

  @Override
  public int hashCode() {
    return getHash(root);
  }

  public static int getHash(Fix fix) {
    return Objects.hash(fix);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Node)) return false;
    Node node = (Node) o;
    return root.equals(node.root);
  }
}
