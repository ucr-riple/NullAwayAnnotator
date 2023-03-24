package test;
import javax.annotation.Nullable;
public class Foo {
   @Nullable Object f1;
   @Nullable Object f2;
   @SuppressWarnings("NullAway.Init") Object f3;
   @SuppressWarnings("NullAway") Object f4 = null;
   Foo() {}
   Foo(int i) {}
   Foo(int i, int j) {}
   void bar1() {
     f3.hashCode();
     f4.hashCode();
   }
   void bar2() {
     f3.hashCode();
     f4.hashCode();
   }
}
