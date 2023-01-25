package test;

public class B extends A {

  Object run(Object p) {
    super.run(p);
    return p;
  }

  void helper(Object h) {
    this.run(h);
  }
}
