package test;

import javax.annotation.Nullable;

public class Bar {

  public void deref() {}

  @Nullable
  public Bar process(Object foo) {
    return null;
  }
}
