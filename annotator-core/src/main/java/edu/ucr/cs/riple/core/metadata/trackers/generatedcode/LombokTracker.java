package edu.ucr.cs.riple.core.metadata.trackers.generatedcode;

import edu.ucr.cs.riple.core.metadata.method.MethodDeclarationTree;
import edu.ucr.cs.riple.core.metadata.trackers.MethodRegionTracker;
import edu.ucr.cs.riple.core.metadata.trackers.Region;
import edu.ucr.cs.riple.scanner.generatedcode.SourceType;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LombokTracker implements GeneratedRegionTracker {

  private final MethodRegionTracker tracker;
  private final MethodDeclarationTree methodDeclarationTree;

  public LombokTracker(
      MethodDeclarationTree methodDeclarationTree, MethodRegionTracker methodRegionTracker) {
    this.methodDeclarationTree = methodDeclarationTree;
    this.tracker = methodRegionTracker;
  }

  @Override
  public Set<Region> extendWithGeneratedRegions(Set<Region> regions) {
    return regions.stream()
        // filter regions where are created by lombok
        .filter(region -> region.sourceType.equals(SourceType.LOMBOK) && region.isOnMethod())
        // find the corresponding method for the region.
        .map(region -> methodDeclarationTree.findNode(region.member, region.clazz))
        .filter(Objects::nonNull)
        // get method location.
        .map(methodNode -> methodNode.location)
        // add potentially impacted regions for the collected methods.
        .flatMap(
            onMethod -> {
              Optional<Set<Region>> ans = tracker.getRegions(onMethod);
              return ans.isPresent() ? ans.get().stream() : Stream.of();
            })
        .collect(Collectors.toSet());
  }
}
