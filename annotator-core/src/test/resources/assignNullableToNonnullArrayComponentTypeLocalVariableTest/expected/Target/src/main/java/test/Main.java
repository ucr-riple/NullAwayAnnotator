package test;
import org.jspecify.annotations.Nullable;
public class Main {
   void run() {
     @Nullable Object[] arr = new Object[1];
     arr[0] = null;
   }
}