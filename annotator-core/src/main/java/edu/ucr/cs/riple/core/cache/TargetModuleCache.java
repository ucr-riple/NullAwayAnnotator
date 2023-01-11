package edu.ucr.cs.riple.core.cache;

import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.metadata.method.MethodDeclarationTree;
import edu.ucr.cs.riple.core.metadata.trackers.CompoundTracker;
import edu.ucr.cs.riple.core.metadata.trackers.RegionTracker;
import edu.ucr.cs.riple.injector.location.Location;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * Cache for storing impacts of fixes on target module. This cache's state is not immutable and can
 * be updated.
 */
public class TargetModuleCache extends BaseCache<Impact, LinkedHashMap<Location, Impact>> {

  public final RegionTracker tracker;

  public TargetModuleCache(Config config, MethodDeclarationTree tree) {
    super(config, new LinkedHashMap<>(), tree);
    this.tracker = new CompoundTracker(config, config.target, tree);
  }

  /**
   * Updates the store with new given information.
   *
   * @param newData New given impacts.
   */
  public void updateCacheState(Set<Impact> newData) {
    newData.forEach(
        t -> {
          if (store.containsKey(t.toLocation())) {
            throw new RuntimeException("Duplicate: " + t.toLocation());
          }
          store.put(t.toLocation(), t);
        });
  }
}
