package edu.ucr.cs.riple.autofixer.metadata.graph;

import static edu.ucr.cs.riple.autofixer.util.Utility.isEqual;

import edu.ucr.cs.riple.autofixer.metadata.trackers.UsageTracker;
import edu.ucr.cs.riple.injector.Fix;
import java.util.List;

public class Node extends AbstractNode {
  public int referred;

  public Node(Fix fix) {
    super(fix);
  }

  public void updateUsages(UsageTracker tracker) {
    this.usages.addAll(tracker.getUsage(this.fix));
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
