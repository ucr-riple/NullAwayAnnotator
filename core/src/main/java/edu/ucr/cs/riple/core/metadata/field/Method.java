package edu.ucr.cs.riple.core.metadata.field;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Method {
  final String signature;
  final Set<String> fields;

  public Method(String signature) {
    this.signature = signature;
    this.fields = new HashSet<>();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Method)) return false;
    Method other = (Method) o;
    return signature.equals(other.signature);
  }

  @Override
  public int hashCode() {
    return Objects.hash(signature);
  }
}
