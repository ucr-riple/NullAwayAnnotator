package test;
import javax.annotation.Nullable;
public class Main {
   Object f;
   Main(Object f) {
      this.f = f;
   }
   Main(Object f, @Nullable Object o) {
      this.f = f;
      Integer h = o.hashCode();
   }
}
class C {
   Main main = new Main(null);
}
