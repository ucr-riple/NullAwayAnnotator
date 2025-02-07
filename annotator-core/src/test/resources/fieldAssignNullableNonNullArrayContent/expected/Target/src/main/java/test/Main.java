package test;
import org.jspecify.annotations.Nullable;

public class Main {
   @Nullable Object[] arr = new Object[1];
   void run() {
      arr[0] = null;
   }
}