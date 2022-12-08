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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.metadata.index.Bank;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.method.MethodDeclarationTree;
import edu.ucr.cs.riple.core.metadata.trackers.Region;
import edu.ucr.cs.riple.core.metadata.trackers.RegionTracker;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.location.OnMethod;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Vertex in {@link ConflictGraph} graph. It stores a fix tree (starting from a root) and all it's
 * impact on the source code information.
 */
public class Node {

  /** Root fix of the tree. */
  public final Fix root;

  /** Set of all fixes in tree. */
  public final Set<Fix> tree;

  /** Set of potentially impacted by any node in tree. */
  public final Set<Region> regions;

  /** Set of triggered fixes if tree is applied. */
  public Set<Fix> triggeredFixes;

  /** Collection of triggered errors if tree is applied. */
  public ImmutableSet<Error> triggeredErrors;

  /** Unique id of Node across all nodes. */
  public int id;

  /** Effect of applying containing change */
  public int effect;

  /**
   * if <code>true</code>, set of triggered fixes has been updated, and the node still needs further
   * process.
   */
  public boolean changed;

  /** Corresponding report of processing root. */
  public Report report;

  /** Regions where original errors reported and NullAway suggested root for that. */
  private Set<Region> origins;

  public Node(Fix root) {
    this.regions = new HashSet<>();
    this.root = root;
    this.triggeredFixes = new HashSet<>();
    this.triggeredErrors = ImmutableSet.of();
    this.effect = 0;
    this.tree = Sets.newHashSet(root);
    this.changed = false;
  }

  /**
   * Initializes rootSource. Collects all regions where error reported from {@link Bank}
   *
   * @param errorBank {@link Bank} instance.
   */
  public void setOrigins(Bank<Error> errorBank) {
    this.origins =
        errorBank.getRegionsForFix(
            error -> error.isSingleFix() && error.toResolvingLocation().equals(root.toLocation()));
  }

  /** Clears origin set. */
  public void clearOrigins() {
    this.origins = Set.of();
  }

  /**
   * It clears the set of regions and will recalculate the potentially impacted regions. Potentially
   * impacted regions are mentioned below:
   *
   * <ul>
   *   <li>All regions that a usage of the set of targeted elements by fixes has been observed.
   *   <li>All regions which can be impacted due to inheritance violation rules.
   *   <li>If a fix is targeting a constructor parameter, class field initialization region will
   *       also be added to this list. (Constructor failures in field initialization, causes errors
   *       to be reported on that class field initialization regions.)
   * </ul>
   *
   * @param tracker Tracker instance.
   */
  public void reCollectPotentiallyImpactedRegions(RegionTracker tracker) {
    this.regions.clear();
    // Add origins.
    this.regions.addAll(this.origins);
    this.tree.forEach(fix -> tracker.getRegions(fix).ifPresent(regions::addAll));
    // Add class initialization region, if a fix is modifying a parameter on constructor.
    this.tree.stream()
        .filter(fix -> fix.isOnParameter() && fix.isModifyingConstructor())
        .forEach(fix -> regions.add(new Region(fix.change.location.clazz, "null")));
  }

  /**
   * Checks if a node has a shared region with this node's regions.
   *
   * @param other Other Node instance.
   * @return true, if there is a conflict and a region is shared.
   */
  public boolean hasConflictInRegions(Node other) {
    return !Collections.disjoint(other.regions, this.regions);
  }

  /**
   * Updates node status. Should be called when all annotations in tree are applied to the source
   * code and the target project has been rebuilt.
   *
   * @param localEffect Local effect calculated based on the number of errors in impacted regions.
   * @param fixesInOneRound All fixes applied simultaneously to the source code.
   * @param triggeredErrors Triggered Errors collected from impacted regions.
   * @param triggeredFixesFromDownstream Set of triggered fixes from downstream.
   * @param mdt Method declaration tree instance.
   */
  public void updateStatus(
      int localEffect,
      Set<Fix> fixesInOneRound,
      Collection<Error> triggeredErrors,
      Set<Fix> triggeredFixesFromDownstream,
      MethodDeclarationTree mdt) {
    // Update list of triggered errors.
    this.triggeredErrors = ImmutableSet.copyOf(triggeredErrors);
    // Update list of triggered fixes.
    this.updateTriggered(triggeredFixesFromDownstream);
    // A fix in a tree, can have a super method that is not part of this node's tree but be present
    // in another node's tree. In this case since both are applied, an error due to inheritance
    // violation will not be reported. This calculation below will fix that.
    final int[] numberOfSuperMethodsAnnotatedOutsideTree = {0};
    this.tree.stream()
        .filter(Fix::isOnMethod)
        .map(
            fix -> {
              OnMethod onMethod = fix.toMethod();
              return mdt.getClosestSuperMethod(onMethod.method, onMethod.clazz);
            }) // List of super methods of all fixes in tree.
        .filter(
            node ->
                node != null
                    && !node.hasNullableAnnotation) // If node is already annotated, ignore it.
        .forEach(
            superMethodNode -> {
              if (this.tree.stream()
                  .anyMatch(
                      fix -> fix.isOnMethod() && fix.toMethod().equals(superMethodNode.location))) {
                // Super method is already inside tree, ignore it.
                return;
              }
              if (fixesInOneRound.stream()
                  .anyMatch(
                      fix -> fix.isOnMethod() && fix.toMethod().equals(superMethodNode.location))) {
                // Super method is not in this tree and is present in source code due to injection
                // for another node, count it.
                numberOfSuperMethodsAnnotatedOutsideTree[0]++;
              }
            });
    // Fix the actual error below.
    this.effect = localEffect + numberOfSuperMethodsAnnotatedOutsideTree[0];
  }

  /**
   * Updated the triggered list and the status of node.
   *
   * @param triggeredFixesFromDownstream Set of triggered fixes from downstream.
   */
  public void updateTriggered(Set<Fix> triggeredFixesFromDownstream) {
    int sizeBefore = this.triggeredFixes.size();
    ImmutableSet<Fix> fixes = Utility.getResolvingFixesOfErrors(this.triggeredErrors);
    this.triggeredFixes.addAll(fixes);
    this.triggeredFixes.addAll(triggeredFixesFromDownstream);
    int sizeAfter = this.triggeredFixes.size();
    this.changed = (sizeAfter != sizeBefore);
  }

  /** Merges triggered fixes to the tree, to prepare the analysis for the next depth. */
  public void mergeTriggered() {
    this.tree.addAll(this.triggeredFixes);
    this.tree.forEach(fix -> fix.fixSourceIsInTarget = true);
    this.triggeredFixes.clear();
  }

  @Override
  public int hashCode() {
    return getHash(root);
  }

  /**
   * Calculates hash. This method is used outside this class to calculate the expected hash based on
   * instance's properties value if the actual instance is not available.
   *
   * @param fix Fix instance.
   * @return Expected hash.
   */
  public static int getHash(Fix fix) {
    return Objects.hash(fix);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Node)) {
      return false;
    }
    Node node = (Node) o;
    return root.equals(node.root);
  }
}
