package edu.ucr.cs.riple.autofixer.metadata.graph;

import static edu.ucr.cs.riple.autofixer.util.Utility.isEqual;

import edu.ucr.cs.riple.autofixer.metadata.UsageTracker;
import edu.ucr.cs.riple.injector.Fix;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Node extends AbstractNode<Node> {
  public final Set<String> classes;
  public final Set<Fix> triggered;
  public int referred;
  public int id;
  public boolean isDangling;

  public Node(Fix fix) {
    super(fix);
    this.isDangling = false;
    this.classes = new HashSet<>();
    this.triggered = new HashSet<>();
  }

  public void updateUsages(UsageTracker tracker) {
    Set<UsageTracker.Usage> usages = tracker.getUsage(this.fix);
    this.usages.addAll(usages);
    this.classes.addAll(usages.stream().map(usage -> usage.clazz).collect(Collectors.toSet()));
    for (UsageTracker.Usage usage : usages) {
      if (usage.method == null || usage.method.equals("null")) {
        isDangling = true;
        break;
      }
    }
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
    if (other.isDangling || this.isDangling) {
      return !Collections.disjoint(other.classes, this.classes);
    }
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
        + ", isDangling="
        + isDangling
        + '}';
  }
}
