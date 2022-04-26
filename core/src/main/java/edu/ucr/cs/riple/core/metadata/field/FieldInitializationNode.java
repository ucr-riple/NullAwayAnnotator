package edu.ucr.cs.riple.core.metadata.field;

import edu.ucr.cs.riple.injector.Location;

public class FieldInitializationNode {

  final Location initializerLocation;
  final String field;

  public FieldInitializationNode(Location initializerLocation, String field) {
    this.initializerLocation = initializerLocation;
    this.field = field;
  }
}
