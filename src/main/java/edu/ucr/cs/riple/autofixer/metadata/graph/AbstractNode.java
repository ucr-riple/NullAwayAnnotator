package edu.ucr.cs.riple.autofixer.metadata.graph;

import edu.ucr.cs.riple.autofixer.metadata.UsageTracker;
import edu.ucr.cs.riple.injector.Fix;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public abstract class AbstractNode {

  public final Fix fix;
  public final Set<UsageTracker.Usage> usages;
  public Set<Fix> triggered;
  public int id;
  public int effect;

  protected AbstractNode(Fix fix) {
    this.usages = new HashSet<>();
    this.fix = fix;
    this.triggered = new HashSet<>();
  }

  public abstract void updateUsages(UsageTracker tracker);

  public boolean hasConflictInUsage(AbstractNode other) {
    return !Collections.disjoint(other.usages, this.usages);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fix.index, fix.className, fix.method);
  }
}
