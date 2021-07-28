package edu.ucr.cs.riple.autofixer.metadata;

import java.util.Objects;

public class FieldUsageNode {
  public final String callerClass;
  public final String callerMethod;
  public final String calleeField;
  public final String calleeClass;

  public FieldUsageNode(
      String callerClass, String callerMethod, String calleeField, String calleeClass) {
    this.callerClass = callerClass;
    this.callerMethod = callerMethod;
    this.calleeField = calleeField;
    this.calleeClass = calleeClass;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof FieldUsageNode)) return false;
    FieldUsageNode that = (FieldUsageNode) o;
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
    return "FieldUsageNode{"
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
