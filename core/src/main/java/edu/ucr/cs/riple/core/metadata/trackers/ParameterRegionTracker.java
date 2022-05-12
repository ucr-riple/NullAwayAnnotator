package edu.ucr.cs.riple.core.metadata.trackers;

import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.method.MethodInheritanceTree;
import edu.ucr.cs.riple.injector.location.OnParameter;
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
    if (!fix.isOnParameter()) {
      return null;
    }
    OnParameter parameter = fix.toParameter();
    Set<Region> regions =
        tree.getSubMethods(parameter.method, parameter.clazz, false)
            .stream()
            .map(node -> new Region(node.method, node.clazz))
            .collect(Collectors.toSet());
    regions.add(new Region(parameter.method, parameter.clazz));
    regions.addAll(methodRegionTracker.getCallersOfMethod(parameter.clazz, parameter.method));
    return regions;
  }
}
