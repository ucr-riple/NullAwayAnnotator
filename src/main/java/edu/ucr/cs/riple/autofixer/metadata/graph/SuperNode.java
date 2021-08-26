package edu.ucr.cs.riple.autofixer.metadata.graph;

import edu.ucr.cs.riple.autofixer.metadata.UsageTracker;
import edu.ucr.cs.riple.injector.Fix;
import java.util.HashSet;
import java.util.Set;

public class SuperNode extends AbstractNode<SuperNode> {

  public final Set<Node> followUps;
  private final Node root;

  public SuperNode(Fix fix) {
    super(fix);
    followUps = new HashSet<>();
    root = new Node(fix);
    followUps.add(root);
  }

  @Override
  public void updateUsages(UsageTracker tracker) {
    for (Node node : followUps) {
      node.updateUsages(tracker);
    }
  }

  @Override
  public boolean hasConflictInUsage(SuperNode other) {
    for (Node node : followUps) {
      for (Node otherNode : other.followUps) {
        if (node.hasConflictInUsage(otherNode)) {
          return true;
        }
      }
    }
    return false;
  }
}
