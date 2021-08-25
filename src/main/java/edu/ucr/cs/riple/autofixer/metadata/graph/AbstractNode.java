package edu.ucr.cs.riple.autofixer.metadata.graph;

import edu.ucr.cs.riple.autofixer.metadata.UsageTracker;
import edu.ucr.cs.riple.injector.Fix;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class AbstractNode<T extends AbstractNode<T>> {

  public final Set<UsageTracker.Usage> usages;
  public final Fix fix;
  public int id;
  public final HashMap<Integer, Set<T>> nodes;

  protected AbstractNode(Fix fix) {
    this.usages = new HashSet<>();
    this.fix = fix;
    nodes = new HashMap<>();
  }

  public void updateUsages(Set<UsageTracker.Usage> usages) {
    throw new UnsupportedOperationException("Cannot Instantiate this class directly");
  }

  public boolean hasConflictInUsage(AbstractNode<T> node) {
    throw new UnsupportedOperationException("Cannot Instantiate this class directly");
  }

  public boolean areSameNode(Fix other) {
    return this.fix.className.equals(other.className)
        && this.fix.method.equals(other.method)
        && this.fix.index.equals(other.index)
        && this.fix.param.equals(other.param);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fix.index, fix.className, fix.method);
  }
}
