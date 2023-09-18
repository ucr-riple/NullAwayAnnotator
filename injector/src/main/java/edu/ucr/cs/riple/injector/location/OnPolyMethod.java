package edu.ucr.cs.riple.injector.location;

import java.nio.file.Path;

public class OnPolyMethod extends Location {

  public OnPolyMethod(Path path, String clazz, String method, int index) {
    super(LocationKind.POLY_METHOD, path, clazz);
  }

  @Override
  public <R, P> R accept(LocationVisitor<R, P> v, P p) {
    return v.visitPolyMethod(this, p);
  }
}
