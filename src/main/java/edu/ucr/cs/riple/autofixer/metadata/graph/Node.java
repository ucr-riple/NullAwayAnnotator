package edu.ucr.cs.riple.autofixer.metadata.graph;

import edu.ucr.cs.riple.autofixer.FixType;
import edu.ucr.cs.riple.autofixer.metadata.trackers.UsageTracker;
import edu.ucr.cs.riple.injector.Fix;

public class Node extends AbstractNode {
  public int referred;

  public Node(Fix fix) {
    super(fix);
  }

  public void updateUsages(UsageTracker tracker) {
    this.usages.addAll(tracker.getUsage(this.fix));
  }

  // We need to subtract referred in METHOD_PARAM since all errors are happening
  // inside the method boundary and all referred sites are outside (at call sites)
  public void setEffect(int localEffect) {
    if (fix.location.equals(FixType.METHOD_PARAM.name)) {
      this.effect = localEffect - this.referred;
    } else {
      this.effect = localEffect;
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
