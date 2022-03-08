package tests;

import javax.annotation.Nullable;

public class CError3 implements I {

  @Override
  @Nullable
  public Object foo() {
    return null;
  }

  public void bar(@Nullable Object foo) {
    String name = foo.toString();
    Integer hash = foo.hashCode();
  }

  public void run() {
    bar(foo());
  }
}
