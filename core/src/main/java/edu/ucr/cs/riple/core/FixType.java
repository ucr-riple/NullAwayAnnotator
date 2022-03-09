package edu.ucr.cs.riple.core;

public enum FixType {
  METHOD_PARAM("PARAMETER"),
  CLASS_FIELD("FIELD"),
  METHOD_RETURN("METHOD");

  public final String name;

  FixType(String name) {
    this.name = name;
  }
}
