package test;
import javax.annotation.Nullable;
public class Bar {
  Object foo = new Object();
  public void deref() {}
  @Nullable
  public Bar process(Object foo) {
    this.foo = foo;
    receiveNonnull(foo);
    return null;
  }
  public Object receiveNonnull(Object o) {
    // just to keep Bar#process:foo @Nonnull
    return o;
  }
}
