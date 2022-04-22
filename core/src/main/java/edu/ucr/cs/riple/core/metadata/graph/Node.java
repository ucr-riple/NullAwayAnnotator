package edu.ucr.cs.riple.core.metadata.graph;

import com.google.common.collect.Sets;
import edu.ucr.cs.riple.core.FixType;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.metadata.index.Bank;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.method.MethodInheritanceTree;
import edu.ucr.cs.riple.core.metadata.method.MethodNode;
import edu.ucr.cs.riple.core.metadata.trackers.Region;
import edu.ucr.cs.riple.core.metadata.trackers.RegionTracker;
import edu.ucr.cs.riple.injector.Location;
import java.util.ArrayList;
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

  /** Effect of applying containing location */
  public int effect;

  /** if <code>true</code>, set of triggered has been updated */
  public boolean changed;

  public Report report;

  /** Regions where error reported * */
  private Set<Region> rootSource;

  public Node(Fix root) {
    this.regions = new HashSet<>();
    this.root = root;
    this.triggered = new HashSet<>();
    this.effect = 0;
    this.tree = Sets.newHashSet(root);
  }

  public void setRootSource(Bank<Fix> fixBank) {
    this.rootSource =
        fixBank.getAllSources(
            o -> {
              if (o.equals(this.root)) {
                return 0;
              }
              return -10;
            });
  }

  public void updateRegions(RegionTracker tracker) {
    this.regions.clear();
    this.regions.addAll(this.rootSource);
    tree.forEach(fix -> regions.addAll(tracker.getRegions(fix)));
  }

  public boolean hasConflictInRegions(Node other) {
    return !Collections.disjoint(other.regions, this.regions);
  }

  public void setEffect(int effect, MethodInheritanceTree mit) {
    Set<Region> subMethodRegions =
        tree.stream()
            .filter(fix -> fix.kind.equals(FixType.PARAMETER.name))
            .flatMap(
                fix ->
                    mit.getSubMethods(fix.method, fix.clazz, false)
                        .stream()
                        .map(methodNode -> new Region(methodNode.method, methodNode.clazz)))
            .filter(region -> !regions.contains(region))
            .collect(Collectors.toSet());
    this.effect = effect + subMethodRegions.size();
  }

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

  @Override
  public int hashCode() {
    return getHash(root);
  }

  public static int getHash(Fix fix) {
    return Objects.hash(fix.variable, fix.index, fix.clazz, fix.method);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Node)) return false;
    Node node = (Node) o;
    return root.equals(node.root);
  }

  /**
   * Generates suggested fixes due to making a parameter {@code Nullable} for all overriding
   * methods.
   *
   * @param mit Method Inheritance Tree.
   * @return List of Fixes
   */
  public List<Fix> generateSubMethodParameterInheritanceFixes(
      MethodInheritanceTree mit, Set<Fix> fixesInOneRound) {
    List<Fix> ans = new ArrayList<>();
    tree.forEach(
        fix -> {
          if (fix.kind.equals(FixType.PARAMETER.name)) {
            ans.addAll(generateSubMethodParameterInheritanceFixesByFix(fix, mit));
          }
        });
    ans.removeAll(fixesInOneRound);
    return ans;
  }

  public void mergeTriggered() {
    this.tree.addAll(this.triggered);
    this.triggered.clear();
  }

  public Set<Fix> getTree() {
    return tree;
  }

  /**
   * Generates suggested fixes due to making a parameter {@code Nullable} for all overriding *
   * methods.
   *
   * @param fix Location containing the location parameter nullable suggestion location.
   * @param mit Method Inheritance Tree.
   * @return List of Fixes
   */
  private static List<Fix> generateSubMethodParameterInheritanceFixesByFix(
      Fix fix, MethodInheritanceTree mit) {
    List<MethodNode> overridingMethods = mit.getSubMethods(fix.method, fix.clazz, false);
    int index = Integer.parseInt(fix.index);
    List<Fix> ans = new ArrayList<>();
    overridingMethods.forEach(
        methodNode -> {
          if (index < methodNode.annotFlags.length && !methodNode.annotFlags[index]) {
            Location location =
                new Location(
                    fix.annotation,
                    fix.method,
                    methodNode.parameterNames[index],
                    FixType.PARAMETER.name,
                    methodNode.clazz,
                    methodNode.uri,
                    "true");
            location.index = String.valueOf(index);
            Fix newFix =
                new Fix(location, "WRONG_OVERRIDE_PARAM", methodNode.clazz, methodNode.method);
            ans.add(newFix);
          }
        });
    return ans;
  }
}
