package test;

public class A {

  boolean a, b, c, d, e, f;

  Object field = new Object();

  public Object run(Object p) {
    if (a) {
      return p;
    }
    if (b) {
      return p;
    }
    if (c) {
      return p;
    }
    if (d) {
      return p;
    }
    if (e) {
      return p;
    }
    if (f) {
      return p;
    }
    this.field = p;
    return new Object();
  }

  public void helper(Object foo) {
    this.run(foo);
  }
}
