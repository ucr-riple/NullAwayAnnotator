package edu.ucr.cs.riple.core.metadata.graph;

import com.google.common.collect.Sets;
import edu.ucr.cs.riple.core.FixType;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.metadata.index.Bank;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.FixEntity;
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

  /** Location to process */
  public final Location root;

  public final Set<Region> regions;

  public Set<Location> triggered;

  public int id;

  /** Effect of applying containing location */
  public int effect;

  /** if <code>true</code>, set of triggered has been updated */
  public boolean changed;

  /** if <code>true</code>, no new triggered error is addressable by a location */
  public boolean finished;

  /** Regions where error reported * */
  protected Set<Region> rootSource;

  public final Set<Location> tree;

  public Report report;

  public Node(Location root) {
    this.regions = new HashSet<>();
    this.root = root;
    this.triggered = new HashSet<>();
    this.effect = 0;
    this.tree = Sets.newHashSet(root);
    this.finished = false;
  }

  public void setRootSource(Bank<FixEntity> fixBank) {
    this.rootSource =
        fixBank.getAllSources(
            o -> {
              if (o.location.equals(this.root)) {
                return 0;
              }
              return -10;
            });
  }

  public void updateUsages(RegionTracker tracker) {
    this.regions.clear();
    this.regions.addAll(this.rootSource);
    tree.forEach(fix -> regions.addAll(tracker.getRegions(fix)));
  }

  public boolean hasConflictInUsage(Node other) {
    return !Collections.disjoint(other.regions, this.regions);
  }

  public void setEffect(int effect, MethodInheritanceTree mit) {
    Set<Region> subMethodRegions =
        tree.stream()
            .filter(fix -> fix.location.equals(FixType.PARAMETER.name))
            .flatMap(
                fix ->
                    mit.getSubMethods(fix.method, fix.clazz, false)
                        .stream()
                        .map(methodNode -> new Region(methodNode.method, methodNode.clazz)))
            .filter(region -> !regions.contains(region))
            .collect(Collectors.toSet());
    this.effect = effect + subMethodRegions.size();
  }

  public void updateTriggered(List<Location> fixes) {
    int sizeBefore = this.triggered.size();
    this.triggered.addAll(fixes);
    int sizeAfter = this.triggered.size();
    for (Location fix : fixes) {
      for (Location other : this.triggered) {
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
    return getHash(root);
  }

  public static int getHash(Location fix) {
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
   * Generates suggested fixes due to making a parameter {@code Nullable} for all overriding *
   * methods.
   *
   * @param fix Location containing the location parameter nullable suggestion location.
   * @param mit Method Inheritance Tree.
   * @return List of Fixes
   */
  protected static List<Location> generateSubMethodParameterInheritanceFixesByFix(
      Location fix, MethodInheritanceTree mit) {
    List<MethodNode> overridingMethods = mit.getSubMethods(fix.method, fix.clazz, false);
    int index = Integer.parseInt(fix.index);
    List<Location> ans = new ArrayList<>();
    overridingMethods.forEach(
        methodNode -> {
          if (index < methodNode.annotFlags.length && !methodNode.annotFlags[index]) {
            Location newFix =
                new Location(
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
  public List<Location> generateSubMethodParameterInheritanceFixes(
      MethodInheritanceTree mit, List<Location> fixesInOneRound) {
    List<Location> ans = new ArrayList<>();
    tree.forEach(
        fix -> {
          if (fix.location.equals(FixType.PARAMETER.name)) {
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

  public Set<Location> getTree() {
    return tree;
  }
}
