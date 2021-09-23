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
    this.followUps.add(root.fix);
  }

  @Override
  public void updateUsages(UsageTracker tracker) {
    this.usages.clear();
    followUps.forEach(fix -> usages.addAll(tracker.getUsage(fix)));
  }

  // Here we do not need to subtract referred for method params since we are observing
  // call sites too.
  public void setEffect(int effect, MethodInheritanceTree tree) {
    final int[] total = {effect};
    followUps.forEach(
        fix -> {
          if (fix.location.equals(FixType.METHOD_PARAM.name)) {
            total[0] += Utility.calculateInheritanceViolationError(tree, fix);
          }
        });
    this.effect = total[0];
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
    this.followUps.addAll(this.triggered);
    this.triggered.clear();
  }
}
