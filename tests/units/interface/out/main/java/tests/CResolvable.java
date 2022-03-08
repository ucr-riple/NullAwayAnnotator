package tests;

import javax.annotation.Nullable;

public class CResolvable implements I {

  @Override
  @Nullable
  public Object foo() {
    return null;
  }

  public void bar(@Nullable Object foo) {}

  public void run() {
    bar(foo());
  }
}
