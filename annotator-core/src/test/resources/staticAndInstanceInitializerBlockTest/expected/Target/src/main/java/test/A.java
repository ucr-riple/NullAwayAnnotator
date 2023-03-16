package test;

import java.util.Objects;
import javax.annotation.Nullable;
import org.jspecify.annotations.NullUnmarked;

public class A {
  @Nullable private static Object f;

  static {
    Object a = B.foo();
    Object b = B.bar();
    if (Objects.hashCode(a) > Objects.hashCode(b)) {
      f = a;
    } else {
      f = b;
    }
  }
}

@NullUnmarked
class B {
  {
    foo().hashCode();
  }

  @Nullable
  public static Object foo() {
    return null;
  }

  @Nullable
  public static Object bar() {
    return null;
  }
}
