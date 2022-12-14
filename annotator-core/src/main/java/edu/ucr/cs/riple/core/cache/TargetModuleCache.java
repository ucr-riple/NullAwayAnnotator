package edu.ucr.cs.riple.core.cache;

import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.metadata.method.MethodDeclarationTree;
import edu.ucr.cs.riple.injector.location.Location;
import java.util.Collection;
import java.util.HashMap;

public class TargetModuleCache<T extends Impact> extends BaseCache<T, HashMap<Location, T>> {

  public TargetModuleCache(Config config, MethodDeclarationTree tree) {
    super(config, new HashMap<>(), tree);
  }

  /**
   * Updates the store with new given information.
   *
   * @param newData New given impacts.
   */
  public void updateCacheState(Collection<T> newData) {
    newData.forEach(t -> store.put(t.toLocation(), t));
  }
}
