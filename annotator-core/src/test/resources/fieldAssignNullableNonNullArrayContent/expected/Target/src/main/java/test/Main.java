package test;

import org.checkerframework.checker.nullness.qual.Nullable;

public class Main {
   @Nullable Object[] arr = new Object[1];
   void run() {
      arr[0] = null;
   }
}