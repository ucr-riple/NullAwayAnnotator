package com.uber;
import javax.annotation.Nullable;

public class Super {
   @Nullable
   Object test() {
       init(this.new NodeVisitor(), this.new EdgeVisitor());

       return foo(this.new Bar(), this.new Foo(), getBuilder().new Foo());
   }
   Object foo(Bar b, Foo f) {
     return Object();
   }
   class Foo{ }
   class Bar{ }
}
