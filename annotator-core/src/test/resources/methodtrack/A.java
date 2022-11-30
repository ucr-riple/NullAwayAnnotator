package test;

public class A extends Root {
  Object foo = new Object();

  Object run(Object p) {
    this.foo = p;
    return p;
  }
}
