package edu.ucr.cs.riple.core.metadata.field;

import java.util.HashSet;
import java.util.Set;

public class Method {
  final String signature;
  final Set<String> fields;

  public Method(String signature) {
    this.signature = signature;
    this.fields = new HashSet<>();
  }
}
