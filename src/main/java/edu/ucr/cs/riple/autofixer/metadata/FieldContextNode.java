package edu.ucr.cs.riple.autofixer.metadata;

import java.util.Objects;

public class FieldContextNode {

  public final String callerClass;
  public final String callerMethod;
  public final String calleeField;
  public final String calleeClass;

  public FieldContextNode(
      String callerClass, String callerMethod, String calleeField, String calleeClass) {
    this.callerClass = callerClass;
    this.callerMethod = callerMethod;
    this.calleeField = calleeField;
    this.calleeClass = calleeClass;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof FieldContextNode)) return false;
    FieldContextNode that = (FieldContextNode) o;
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
    return "FieldContextNode{"
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
