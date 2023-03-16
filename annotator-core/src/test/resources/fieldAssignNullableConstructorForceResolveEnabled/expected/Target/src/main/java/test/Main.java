package test;

import javax.annotation.Nullable;
import org.jspecify.annotations.NullUnmarked;

public class Main {
  Object f;

  @NullUnmarked
  Main(Object f) {
    this.f = f;
  }

  @NullUnmarked
  Main(Object f, @Nullable Object o) {
    this.f = f;
    Integer h = o.hashCode();
  }
}

class C {
  Main main = new Main(null);
}
