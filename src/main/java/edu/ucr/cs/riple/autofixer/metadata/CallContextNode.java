package edu.ucr.cs.riple.autofixer.metadata;

import java.util.Objects;

public class CallContextNode {
  public final String callerClass;
  public final String callerMethod;
  public final String calleeMethod;
  public final String calleeClass;

  public CallContextNode(
      String callerClass, String callerMethod, String calleeMethod, String calleeClass) {
    this.callerClass = callerClass;
    this.calleeMethod = calleeMethod;
    this.calleeClass = calleeClass;
    this.callerMethod = callerMethod;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof CallContextNode)) return false;
    CallContextNode that = (CallContextNode) o;
    return callerClass.equals(that.callerClass)
        && calleeMethod.equals(that.calleeMethod)
        && calleeClass.equals(that.calleeClass);
  }

  @Override
  public int hashCode() {
    return Objects.hash(calleeMethod, calleeClass);
  }

  @Override
  public String toString() {
    return "CallGraphDisplay{"
        + "callerClass='"
        + callerClass
        + '\''
        + ", calleeMethod='"
        + calleeMethod
        + '\''
        + ", calleeClass='"
        + calleeClass
        + '\''
        + '}';
  }
}
