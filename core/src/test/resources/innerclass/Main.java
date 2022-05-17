package test;

public class Main {

  public void run() {
    Foo foo =
        new Foo() {
          Object f = null;

          @Override
          public Object bar(Object o) {
            return null;
          }
        };
    class Child extends Base {
      public Object exec() {
        return null;
      }
    }
  }
}

class Base {
  public Object exec() {
    return new Object();
  }
}
