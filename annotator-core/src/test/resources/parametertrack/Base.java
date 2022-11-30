package test;

public class Base {

  Object field = new Object();

  public Object run(Object f) {
    this.field = f;
    return f;
  }
}
