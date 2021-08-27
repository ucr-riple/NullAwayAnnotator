package edu.ucr.cs.riple.autofixer.metadata.graph;

import edu.ucr.cs.riple.autofixer.Report;
import edu.ucr.cs.riple.autofixer.metadata.UsageTracker;
import edu.ucr.cs.riple.injector.Fix;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class SuperNode extends AbstractNode {

  public final Set<Node> followUps;
  public Report report;
  private final Node root;

  public SuperNode(Fix fix) {
    super(fix);
    followUps = new HashSet<>();
    root = new Node(fix);
    followUps.add(root);
  }

  @Override
  public void updateUsages(UsageTracker tracker) {
    followUps.forEach(node -> usages.addAll(tracker.getUsage(node.fix)));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SuperNode)) return false;
    SuperNode superNode = (SuperNode) o;
    return root.equals(superNode.root);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), followUps, report, root);
  }

  @Override
  public String toString() {
    return "SuperNode{" + "followUps=" + followUps + ", report=" + report + ", root=" + root + '}';
  }

  public Set<Fix> getFixChain() {
    return followUps.stream().map(node -> node.fix).collect(Collectors.toSet());
  }

  public void mergeTriggered() {
    this.followUps.addAll(triggered.stream().map(Node::new).collect(Collectors.toSet()));
    this.triggered.clear();
  }
}
