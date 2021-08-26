package edu.ucr.cs.riple.autofixer.metadata.graph;

import static edu.ucr.cs.riple.autofixer.util.Utility.isEqual;

import edu.ucr.cs.riple.autofixer.metadata.UsageTracker;
import edu.ucr.cs.riple.injector.Fix;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class Node extends AbstractNode<Node> {
  public int referred;

  public Node(Fix fix) {
    super(fix);
  }

  public void updateUsages(UsageTracker tracker) {
    Set<UsageTracker.Usage> usages = tracker.getUsage(this.fix);
    this.usages.addAll(usages);
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

  public boolean hasConflictInUsage(Node other) {
    return !Collections.disjoint(other.usages, this.usages);
  }

  @Override
  public String toString() {
    return "Node{"
        + "fix=["
        + fix.method
        + " "
        + fix.className
        + " "
        + fix.param
        + " "
        + fix.location
        + "]"
        + ", id="
        + id
        + ", effect="
        + effect
        + ", referred="
        + referred
        + '}';
  }
}
