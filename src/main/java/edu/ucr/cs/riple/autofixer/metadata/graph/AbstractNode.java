package edu.ucr.cs.riple.autofixer.metadata.graph;

import edu.ucr.cs.riple.autofixer.metadata.UsageTracker;
import edu.ucr.cs.riple.injector.Fix;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public abstract class AbstractNode<T extends AbstractNode<T>> {

  public final Fix fix;
  public final Set<UsageTracker.Usage> usages;
  public int id;
  public int effect;

  protected AbstractNode(Fix fix) {
    this.usages = new HashSet<>();
    this.fix = fix;
  }

  public abstract void updateUsages(UsageTracker tracker);

  public abstract boolean hasConflictInUsage(T other);

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
