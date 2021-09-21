package edu.ucr.cs.riple.autofixer.metadata.graph;

import edu.ucr.cs.riple.autofixer.FixType;
import edu.ucr.cs.riple.autofixer.Report;
import edu.ucr.cs.riple.autofixer.metadata.method.MethodInheritanceTree;
import edu.ucr.cs.riple.autofixer.metadata.trackers.UsageTracker;
import edu.ucr.cs.riple.autofixer.util.Utility;
import edu.ucr.cs.riple.injector.Fix;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class SuperNode extends AbstractNode {

  public final Set<Fix> followUps;
  public Report report;
  private final Node root;

  public SuperNode(Fix fix) {
    super(fix);
    followUps = new HashSet<>();
    root = new Node(fix);
    root.referred++;
    this.followUps.add(root.fix);
  }

  @Override
  public void updateUsages(UsageTracker tracker) {
    this.usages.clear();
    followUps.forEach(fix -> usages.addAll(tracker.getUsage(fix)));
  }

  public void setEffect(int effect, MethodInheritanceTree tree) {
    final int[] total = {effect};
    followUps.forEach(
        fix -> {
          if (fix.location.equals(FixType.METHOD_PARAM.name)) {
            total[0] += Utility.calculateInheritanceViolationError(tree, fix);
            total[0] -= fix.referred;
          }
        });
    this.effect = total[0];
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
    return followUps;
  }

  public void mergeTriggered() {
    this.addFollowUps(this.triggered);
    this.triggered.clear();
  }

  public void addFollowUps(Set<Fix> fixes) {
    followUps.addAll(fixes);
  }
}
