package edu.ucr.cs.riple.core.metadata.graph;

import edu.ucr.cs.riple.core.FixType;
import edu.ucr.cs.riple.core.metadata.method.MethodInheritanceTree;
import edu.ucr.cs.riple.core.metadata.trackers.UsageTracker;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.Fix;

public class Node extends AbstractNode {

  public Node(Fix fix) {
    super(fix);
  }

  public void updateUsages(UsageTracker tracker) {
    this.usages.addAll(tracker.getUsage(this.fix));
  }

  // We need to subtract referred in METHOD_PARAM since all errors are happening
  // inside the method boundary and all referred sites are outside (at call sites)
  @Override
  public void setEffect(int localEffect, MethodInheritanceTree tree) {
    if (fix.location.equals(FixType.METHOD_PARAM.name)) {
      this.effect =
          localEffect
              - this.fix.referred
              + Utility.calculateInheritanceViolationError(tree, this.fix);
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
        + fix.referred
        + '}';
  }
}
