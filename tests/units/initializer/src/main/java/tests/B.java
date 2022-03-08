package tests;

public abstract class B {
  protected Baz foo;
  private Object item;

  enum Baz {
    Bar
  }

  public void notItemInitializer() {
    if (item == null) {
      throw new RuntimeException("Whoops");
    }
  }

  public boolean isBar() {
    return this.foo == Baz.Bar; // where Baz is an enum
  }
}

class A extends B {
  public A() {
    this.foo = Baz.Bar; // non-null expr
  }
}
