package edu.ucr.cs.riple.autofixer.metadata;

import java.util.Objects;

public class FieldNode {
  public final String callerClass;
  public final String callerMethod;
  public final String calleeField;
  public final String calleeClass;

  public FieldNode(
      String callerClass, String callerMethod, String calleeField, String calleeClass) {
    this.callerClass = callerClass;
    this.callerMethod = callerMethod;
    this.calleeField = calleeField;
    this.calleeClass = calleeClass;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof FieldNode)) return false;
    FieldNode that = (FieldNode) o;
    return callerClass.equals(that.callerClass)
        && calleeField.equals(that.calleeField)
        && calleeClass.equals(that.calleeClass);
  }

  @Override
  public int hashCode() {
    return Objects.hash(calleeField, calleeClass);
  }

  @Override
  public String toString() {
    return "FieldNode{"
        + "callerClass='"
        + callerClass
        + '\''
        + ", calleeField='"
        + calleeField
        + '\''
        + ", calleeClass='"
        + calleeClass
        + '\''
        + '}';
  }
}
