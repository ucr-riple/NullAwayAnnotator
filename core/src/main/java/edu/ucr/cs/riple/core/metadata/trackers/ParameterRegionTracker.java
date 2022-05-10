package edu.ucr.cs.riple.core.metadata.trackers;

import edu.ucr.cs.riple.core.FixType;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.method.MethodInheritanceTree;
import java.util.Set;
import java.util.stream.Collectors;

public class ParameterRegionTracker implements RegionTracker {

  private final MethodInheritanceTree tree;
  private final MethodRegionTracker methodRegionTracker;

  public ParameterRegionTracker(
      MethodInheritanceTree tree, MethodRegionTracker methodRegionTracker) {
    this.tree = tree;
    this.methodRegionTracker = methodRegionTracker;
  }

  @Override
  public Set<Region> getRegions(Fix fix) {
    if (!fix.kind.equals(FixType.PARAMETER.name)) {
      return null;
    }
    Set<Region> regions =
        tree.getSubMethods(fix.method, fix.clazz, false)
            .stream()
            .map(node -> new Region(node.method, node.clazz))
            .collect(Collectors.toSet());
    regions.add(new Region(fix.method, fix.clazz));
    regions.addAll(methodRegionTracker.getCallersOfMethod(fix.clazz, fix.method));
    return regions;
  }
}
