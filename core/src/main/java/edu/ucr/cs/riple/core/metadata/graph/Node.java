package edu.ucr.cs.riple.core.metadata.graph;

import com.google.common.collect.Sets;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.metadata.index.Bank;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.method.MethodInheritanceTree;
import edu.ucr.cs.riple.core.metadata.trackers.Region;
import edu.ucr.cs.riple.core.metadata.trackers.RegionTracker;
import edu.ucr.cs.riple.injector.Change;
import edu.ucr.cs.riple.injector.location.OnMethod;
import edu.ucr.cs.riple.injector.location.OnParameter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
    triggered.addAll(generateSubMethodParameterInheritanceFixes(mit, fixesInOneRound));
    updateTriggered(triggered);
    final int[] numberOfSuperMethodsAnnotatedOutsideTree = {0};
    this.tree
        .stream()
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
              if (fixesInOneRound
                  .stream()
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

  /**
   * Generates suggested fixes due to making a parameter {@code Nullable} for all overriding
   * methods.
   *
   * @param mit OnMethod Inheritance Tree.
   * @return List of Fixes
   */
  private List<Fix> generateSubMethodParameterInheritanceFixes(
      MethodInheritanceTree mit, Set<Fix> fixesInOneRound) {
    return tree.stream()
        .filter(Fix::isOnParameter)
        .flatMap(
            fix -> {
              OnParameter p = fix.toParameter();
              return mit.getSubMethods(p.method, p.clazz, false)
                  .stream()
                  .filter(
                      methodNode ->
                          (p.index < methodNode.annotFlags.length
                              && !methodNode.annotFlags[p.index]))
                  .map(
                      node -> {
                        Change change =
                            new Change(
                                new OnParameter(
                                    node.uri, node.clazz, p.method, p.parameter, p.index),
                                fix.annotation,
                                true);
                        return new Fix(change, "WRONG_OVERRIDE_PARAM", node.clazz, node.method);
                      });
            })
        .filter(fix -> !fixesInOneRound.contains(fix))
        .collect(Collectors.toList());
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
