package edu.ucr.cs.riple.core.metadata.field;

import java.util.HashSet;
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
}
