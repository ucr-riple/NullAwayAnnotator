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

package edu.ucr.cs.riple.core.evaluators.graph;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.module.ModuleInfo;
import edu.ucr.cs.riple.core.registries.index.Error;
import edu.ucr.cs.riple.core.registries.index.ErrorStore;
import edu.ucr.cs.riple.core.registries.index.Fix;
import edu.ucr.cs.riple.core.registries.region.Region;
import edu.ucr.cs.riple.core.registries.region.RegionRegistry;
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
  /** Set of triggered errors if tree is applied on target module. */
  public ImmutableSet<Error> triggeredErrors;
  /**
   * Set of triggered fixes on target module that will be triggered if fix tree is applied due to
   * errors in downstream dependencies.
   */
  public ImmutableSet<Fix> triggeredFixesFromDownstreamErrors;
  /** Unique id of Node across all nodes. */
  public int id;
  /** Effect of applying containing change */
  public int effect;
  /** Corresponding report of processing root. */
  public Report report;
  /** Regions where original errors reported and NullAway suggested root for that. */
  private ImmutableSet<Region> origins;

  public Node(Fix root) {
    this.regions = new HashSet<>();
    this.root = root;
    this.triggeredFixesFromDownstreamErrors = ImmutableSet.of();
    this.triggeredErrors = ImmutableSet.of();
    this.effect = 0;
    this.tree = Sets.newHashSet(root);
    this.origins = ImmutableSet.of();
  }

  /**
   * Initializes rootSource. Collects all regions where error reported from {@link ErrorStore}
   *
   * @param errorStore {@link ErrorStore} instance.
   */
  public void setOrigins(ErrorStore errorStore) {
    this.origins =
        ImmutableSet.copyOf(
            errorStore.getRegionsForElements(error -> error.getResolvingFixes().contains(root)));
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
   * @param regionRegistry Region registry instance. Used to retrieve regions that can be
   *     potentially impacted by the changes in this node.
   */
  public void reCollectPotentiallyImpactedRegions(RegionRegistry regionRegistry) {
    this.regions.clear();
    // Add origins.
    this.regions.addAll(this.origins);
    this.tree.forEach(
        fix -> this.regions.addAll(regionRegistry.getImpactedRegions(fix.toLocation())));
    // Add class initialization region, if a fix is modifying a parameter on constructor.
    this.tree.stream()
        .filter(fix -> fix.isOnParameter() && fix.isModifyingConstructor())
        .forEach(fix -> regions.add(new Region(fix.change.getLocation().clazz, "null")));
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
   * @param triggeredFixesFromDownstreamErrors Triggered fixes from downstream dependencies.
   * @param triggeredErrors Triggered Errors collected from impacted regions.
   * @param moduleInfo {@link ModuleInfo} instance.
   */
  public void updateStatus(
      int localEffect,
      Set<Fix> fixesInOneRound,
      Collection<Fix> triggeredFixesFromDownstreamErrors,
      Collection<Error> triggeredErrors,
      ModuleInfo moduleInfo) {
    // Update list of triggered fixes on downstream.
    this.triggeredFixesFromDownstreamErrors =
        ImmutableSet.copyOf(triggeredFixesFromDownstreamErrors);
    // Update set of triggered errors.
    this.triggeredErrors = ImmutableSet.copyOf(triggeredErrors);
    // A fix in a tree, can have a super method that is not part of this node's tree but be present
    // in another node's tree. In this case since both are applied, an error due to inheritance
    // violation will not be reported. This calculation below will fix that.
    final int[] numberOfSuperMethodsAnnotatedOutsideTree = {0};
    this.tree.stream()
        .filter(Fix::isOnMethod)
        .map(
            fix -> {
              OnMethod onMethod = fix.toMethod();
              return moduleInfo.getMethodRegistry().getImmediateSuperMethod(onMethod);
            }) // Collection of super methods of all fixes in tree.
        .filter(
            node ->
                node != null
                    && !node.hasNullableAnnotation()) // If node is already annotated, ignore it.
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

  /** Merges triggered fixes to the tree, to prepare the analysis for the next depth. */
  public void mergeTriggered() {
    this.tree.addAll(Error.getResolvingFixesOfErrors(this.triggeredErrors));
    this.tree.addAll(triggeredFixesFromDownstreamErrors);
    this.tree.forEach(fix -> fix.fixSourceIsInTarget = true);
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
