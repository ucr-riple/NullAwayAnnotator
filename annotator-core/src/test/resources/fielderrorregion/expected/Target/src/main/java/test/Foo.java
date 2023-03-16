package test;

import javax.annotation.Nullable;

public class Foo {
  @Nullable Bar f0 = new Bar().process(null);

  @SuppressWarnings("NullAway.Init")
  Bar f1;

  @SuppressWarnings("NullAway.Init")
  Bar f2, f3;
  @Nullable Bar nullableBar;

  @SuppressWarnings("NullAway")
  Bar f4 = nullableBar.process(new Object());

  @Nullable Bar f5;

  Foo() {
    this.f2 = new Bar();
  }

  public void run1() {
    // to prevent annotator making f1, f3 and f4 @Nullable.
    f1.deref();
    f3.deref();
    f4.deref();
  }

  public void run2() {
    // to prevent annotator making f1, f3 and f4 @Nullable.
    f1.deref();
    f3.deref();
    f4.deref();
  }
}
