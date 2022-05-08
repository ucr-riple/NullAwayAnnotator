package edu.ucr.cs.riple.core.metadata.field;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class FieldDeclarationInfo {
  public final Set<String> fields;
  public final String clazz;

  public FieldDeclarationInfo(String clazz) {
    this.clazz = clazz;
    this.fields = new HashSet<>();
  }

  public boolean containsField(String field) {
    return fields.contains(field);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof FieldDeclarationInfo)) return false;
    FieldDeclarationInfo info = (FieldDeclarationInfo) o;
    return clazz.equals(info.clazz) && fields.equals(info.fields);
  }

  @Override
  public int hashCode() {
    return Objects.hash(clazz);
  }
}
