package tests;

import javax.annotation.Nullable;

public class C implements I {

  @Override
  @Nullable
  public Object foo() {
    return null;
  }
}
