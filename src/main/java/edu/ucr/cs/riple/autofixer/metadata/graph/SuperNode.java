package edu.ucr.cs.riple.autofixer.metadata.graph;

import edu.ucr.cs.riple.autofixer.metadata.UsageTracker;
import edu.ucr.cs.riple.injector.Fix;
import java.util.Set;

public class SuperNode extends AbstractNode<SuperNode> {
  protected SuperNode(Fix fix) {
    super(fix);
  }

  @Override
  public void updateUsages(Set<UsageTracker.Usage> usages) {}

  @Override
  public boolean hasConflictInUsage(SuperNode node) {
    return false;
  }
}
