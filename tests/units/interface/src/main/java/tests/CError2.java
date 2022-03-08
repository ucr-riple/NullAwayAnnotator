package tests;

public class CError2 implements I {

  @Override
  public Object foo() {
    return null;
  }

  public void bar(Object foo) {
    String name = foo.toString();
    Integer hash = foo.hashCode();
  }

  public void run() {
    bar(foo());
  }
}
