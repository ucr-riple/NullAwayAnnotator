package com.uber;
import javax.annotation.Nullable;
public class Super {
   @Nullable
   Object test(boolean flag) {
       return new Object();
   }
   class SuperInner {
       Object bar(@Nullable Object foo) {
           return foo;
       }
   }
}
