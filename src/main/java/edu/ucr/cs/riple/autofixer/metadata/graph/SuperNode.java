package edu.ucr.cs.riple.autofixer.metadata.graph;

import edu.ucr.cs.riple.autofixer.metadata.UsageTracker;
import edu.ucr.cs.riple.injector.Fix;

import java.util.HashSet;
import java.util.Set;

public class SuperNode extends AbstractNode<SuperNode> {

  public final Set<Fix> followUps;

  public SuperNode(Fix fix) {
    super(fix);
    followUps = new HashSet<>();
  }

  @Override
  public void updateUsages(UsageTracker tracker) {}

  @Override
  public boolean hasConflictInUsage(SuperNode node) {
    return false;
  }
}
