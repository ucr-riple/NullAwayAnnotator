package edu.ucr.cs.riple.core.model;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.method.MethodDeclarationTree;
import edu.ucr.cs.riple.injector.location.Location;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;

public abstract class BaseModel<T extends Impact, S extends Map<Location, T>> implements Model<T> {

  protected final S store;
  protected final Config config;
  protected final MethodDeclarationTree tree;

  public BaseModel(Config config, S store, MethodDeclarationTree tree) {
    this.store = store;
    this.config = config;
    this.tree = tree;
  }

  @Override
  public boolean isUnknown(Fix fix) {
    return !this.store.containsKey(fix.toLocation());
  }

  @Nullable
  @Override
  public T fetchImpact(Fix fix) {
    return store.getOrDefault(fix.toLocation(), null);
  }

  @Override
  public ImmutableSet<Error> getTriggeredErrorsForCollection(Collection<Fix> fixes) {
    return fixes.stream()
        .map(fix -> store.get(fix.toLocation()))
        .filter(Objects::nonNull)
        .flatMap(impact -> impact.triggeredErrors.stream())
        // filter errors that will be resolved with the existing collection of fixes.
        .filter(error -> !error.isResolvableWith(fixes))
        .collect(ImmutableSet.toImmutableSet());
  }

  @Override
  public ImmutableSet<Error> getTriggeredErrors(Fix fix) {
    T impact = fetchImpact(fix);
    return impact == null ? ImmutableSet.of() : ImmutableSet.copyOf(impact.getTriggeredErrors());
  }

  @Override
  public void updateImpactsAfterInjection(Collection<Fix> fixes) {
    this.store.values().forEach(methodImpact -> methodImpact.updateStatusAfterInjection(fixes));
  }

  @Override
  public boolean triggeresUnresolvableErrors(Fix fix) {
    return getTriggeredErrors(fix).stream().anyMatch(error -> !error.isFixableOnTarget(tree));
  }
}
