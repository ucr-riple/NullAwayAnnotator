package edu.ucr.cs.riple.autofixer.metadata.trackers;

import java.util.Objects;

public class TrackerNode {
  public final String callerClass;
  public final String callerMethod;
  public final String calleeMember;
  public final String calleeClass;

  public TrackerNode(
      String callerClass, String callerMethod, String calleeMember, String calleeClass) {
    this.callerClass = callerClass;
    this.callerMethod = callerMethod;
    this.calleeMember = calleeMember;
    this.calleeClass = calleeClass;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TrackerNode)) return false;
    TrackerNode that = (TrackerNode) o;
    return callerClass.equals(that.callerClass)
        && calleeMember.equals(that.calleeMember)
        && calleeClass.equals(that.calleeClass)
        && callerMethod.equals(that.callerMethod);
  }

  @Override
  public int hashCode() {
    return Objects.hash(calleeMember, calleeClass);
  }

  @Override
  public String toString() {
    return "CallNode{"
        + "callerClass='"
        + callerClass
        + '\''
        + ", callerMethod='"
        + callerMethod
        + '\''
        + ", calleeMember='"
        + calleeMember
        + '\''
        + ", calleeClass='"
        + calleeClass
        + '\''
        + '}';
  }
}
