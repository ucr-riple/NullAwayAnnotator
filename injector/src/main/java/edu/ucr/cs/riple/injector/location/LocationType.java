package edu.ucr.cs.riple.injector.location;

public enum LocationType {
  FIELD,
  METHOD,
  PARAMETER;

  public static LocationType getType(String type) {
    if (type.equalsIgnoreCase("field")) {
      return FIELD;
    }
    if (type.equalsIgnoreCase("method")) {
      return METHOD;
    }
    if (type.equalsIgnoreCase("parameter")) {
      return PARAMETER;
    }
    throw new UnsupportedOperationException("Cannot detect type: " + type);
  }
}
