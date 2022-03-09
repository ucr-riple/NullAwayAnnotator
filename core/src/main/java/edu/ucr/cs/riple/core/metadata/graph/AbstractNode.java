package edu.ucr.cs.riple.core.metadata.graph;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.method.MethodInheritanceTree;
import edu.ucr.cs.riple.core.metadata.trackers.Usage;
import edu.ucr.cs.riple.core.metadata.trackers.UsageTracker;
import edu.ucr.cs.riple.injector.Fix;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public abstract class AbstractNode {

  /** Fix to process */
  public final Fix fix;

  public final Set<Usage> usages;
  public Set<Fix> triggered;
  public int id;
  /** Effect of applying containing fix */
  public int effect;
  /** if <code>true</code>, set of triggered has been updated */
  public boolean changed;
  /** if <code>true</code>, no new triggered error is addressable by a fix */
  public boolean finished;

  protected AbstractNode(Fix fix) {
    this.usages = new HashSet<>();
    this.fix = fix;
    this.triggered = new HashSet<>();
    this.effect = 0;
    this.finished = false;
  }

  public abstract void updateUsages(UsageTracker tracker);

  public boolean hasConflictInUsage(AbstractNode other) {
    return !Collections.disjoint(other.usages, this.usages);
  }

  public abstract void setEffect(int localEffect, MethodInheritanceTree tree);

  public void updateTriggered(List<Fix> fixes) {
    int sizeBefore = this.triggered.size();
    this.triggered.addAll(fixes);
    int sizeAfter = this.triggered.size();
    for (Fix fix : fixes) {
      for (Fix other : this.triggered) {
        if (fix.equals(other)) {
          other.referred++;
          break;
        }
      }
    }
    changed = sizeAfter != sizeBefore;
  }

  public void analyzeStatus(List<Error> newErrors) {
    if (this.finished) {
      return;
    }
    this.finished = newErrors.stream().noneMatch(this::isFixableError);
  }

  private boolean isFixableError(Error error) {
    final Set<String> fixableTypes =
        ImmutableSet.of(
            "METHOD_NO_INIT",
            "FIELD_NO_INIT",
            "ASSIGN_FIELD_NULLABLE",
            "ASSIGN_FIELD_NULLABLE",
            "RETURN_NULLABLE",
            "WRONG_OVERRIDE_RETURN",
            "ASSIGN_FIELD_NULLABLE",
            "PASS_NULLABLE");
    if (fixableTypes.contains(error.messageType)) {
      return true;
    }
    final String unfixableWrongOverrideParamMessage =
        "unbound instance method reference cannot be used, as first parameter of functional interface method";
    return error.messageType.equals("WRONG_OVERRIDE_PARAM")
        && !error.message.contains(unfixableWrongOverrideParamMessage);
  }

  @Override
  public int hashCode() {
    return getHash(fix);
  }

  public static int getHash(Fix fix) {
    return Objects.hash(fix.param, fix.index, fix.className, fix.method);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AbstractNode)) return false;
    AbstractNode node = (AbstractNode) o;
    return fix.equals(node.fix);
  }
}
