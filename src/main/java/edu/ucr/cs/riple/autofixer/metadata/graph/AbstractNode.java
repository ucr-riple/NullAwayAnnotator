package edu.ucr.cs.riple.autofixer.metadata.graph;

import static edu.ucr.cs.riple.autofixer.util.Utility.isEqual;

import edu.ucr.cs.riple.autofixer.metadata.index.Error;
import edu.ucr.cs.riple.autofixer.metadata.trackers.UsageTracker;
import edu.ucr.cs.riple.injector.Fix;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public abstract class AbstractNode {

  public final Fix fix;
  public final Set<UsageTracker.Usage> usages;
  public Set<Fix> triggered;
  public List<Error> newErrors;
  public int id;
  public int effect;

  protected AbstractNode(Fix fix) {
    this.usages = new HashSet<>();
    this.fix = fix;
    this.triggered = new HashSet<>();
    this.effect = 0;
    this.newErrors = new ArrayList<>();
  }

  public abstract void updateUsages(UsageTracker tracker);

  public boolean hasConflictInUsage(AbstractNode other) {
    return !Collections.disjoint(other.usages, this.usages);
  }

  public void updateTriggered(List<Fix> fixes) {
    this.triggered.addAll(fixes);
    for (Fix fix : fixes) {
      for (Fix other : this.triggered) {
        if (isEqual(fix, other)) {
          other.referred++;
          break;
        }
      }
    }
  }

  @Override
  public int hashCode() {
    return getHash(fix);
  }

  public static int getHash(Fix fix) {
    return Objects.hash(fix.index, fix.className, fix.method);
  }
}
