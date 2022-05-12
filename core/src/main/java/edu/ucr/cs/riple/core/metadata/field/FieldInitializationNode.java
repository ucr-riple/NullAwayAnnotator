package edu.ucr.cs.riple.core.metadata.field;

import edu.ucr.cs.riple.injector.location.Location;
import java.util.Objects;

public class FieldInitializationNode {

  final Location initializerLocation;
  final String field;

  public FieldInitializationNode(Location initializerLocation, String field) {
    this.initializerLocation = initializerLocation;
    this.field = field;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof FieldInitializationNode)) return false;
    FieldInitializationNode that = (FieldInitializationNode) o;
    return initializerLocation.equals(that.initializerLocation) && field.equals(that.field);
  }

  @Override
  public int hashCode() {
    return Objects.hash(initializerLocation.clazz, field);
  }
}
