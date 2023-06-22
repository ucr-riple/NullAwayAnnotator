package test;
import javax.annotation.Nullable;
import org.jspecify.annotations.NullUnmarked;
public class Bar {
  Object foo = new Object();
  public void deref() {}
  @NullUnmarked @Nullable
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
