package injector;
import javax.annotation.Nullable;


public class Main {

  public void foo1() {
    class Helper {
      Object f1;
    }
  }

  class Helper {
    Object f0;
  }

  public void foo() {
    class Helper {
      @Nullable
      Object f2;
    }
  }
}
