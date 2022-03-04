package edu.ucr.cs.riple.core;

public enum FixType {
  METHOD_PARAM("METHOD_PARAM"),
  CLASS_FIELD("CLASS_FIELD"),
  METHOD_RETURN("METHOD_RETURN");

  public final String name;

  FixType(String name) {
    this.name = name;
  }
}
