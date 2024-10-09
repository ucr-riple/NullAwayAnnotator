/*
 * MIT License
 *
 * Copyright (c) 2020 Nima Karimipour
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package edu.ucr.cs.riple.injector;

import edu.ucr.cs.riple.injector.changes.AddMarkerAnnotation;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.injector.location.OnMethod;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class LexicalPreservationTest extends BaseInjectorTest {
  @Test
  public void saveImports() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import static com.ibm.wala.types.TypeName.ArrayMask;",
            "import static com.ibm.wala.types.TypeName.ElementBits;",
            "import static com.ibm.wala.types.TypeName.PrimitiveMask;",
            "import com.ibm.wala.types.TypeName.IntegerMask;",
            "import com.ibm.wala.util.collections.HashMapFactory;",
            "import java.io.Serializable;",
            "import java.util.Map;",
            "public class Foo {",
            "   Object test() {",
            "       return new Object();",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import static com.ibm.wala.types.TypeName.ArrayMask;",
            "import static com.ibm.wala.types.TypeName.ElementBits;",
            "import static com.ibm.wala.types.TypeName.PrimitiveMask;",
            "import com.ibm.wala.types.TypeName.IntegerMask;",
            "import com.ibm.wala.util.collections.HashMapFactory;",
            "import java.io.Serializable;",
            "import java.util.Map;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   @Nullable Object test() {",
            "       return new Object();",
            "   }",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnMethod("Foo.java", "test.Foo", "test()"), "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void saveImportsAsterisk() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import static com.ibm.wala.types.A;",
            "import static com.ibm.wala.types.B;",
            "import static com.ibm.wala.types.C;",
            "import static com.ibm.wala.types.D;",
            "import static com.ibm.wala.types.E;",
            "import static com.ibm.wala.types.F;",
            "import static com.ibm.wala.types.G;",
            "import static com.ibm.wala.types.H;",
            "import static com.ibm.wala.types.I;",
            "import static com.ibm.wala.types.J;",
            "import static com.ibm.wala.types.K;",
            "import static com.ibm.wala.types.L;",
            "import com.ibm.wala.util.A;",
            "import com.ibm.wala.util.B;",
            "import com.ibm.wala.util.C;",
            "import com.ibm.wala.util.D;",
            "import com.ibm.wala.util.E;",
            "import com.ibm.wala.util.F;",
            "import com.ibm.wala.util.G;",
            "import com.ibm.wala.util.H;",
            "import com.ibm.wala.util.I;",
            "import com.ibm.wala.util.J;",
            "import com.ibm.wala.util.K;",
            "import com.ibm.wala.util.L;",
            "import com.ibm.wala.util.M;",
            "import com.ibm.wala.util.N;",
            "import com.ibm.wala.util.O;",
            "import com.ibm.wala.util.P;",
            "import com.ibm.wala.util.Q;",
            "import com.ibm.wala.util.R;",
            "import com.ibm.wala.util.S;",
            "import com.ibm.wala.util.T;",
            "import com.ibm.wala.util.U;",
            "import com.ibm.wala.util.V;",
            "import com.ibm.wala.util.W;",
            "import com.ibm.wala.util.X;",
            "import com.ibm.wala.util.Y;",
            "import com.ibm.wala.util.Z;",
            "import com.ibm.wala.util.AA;",
            "import com.ibm.wala.util.AB;",
            "public class Foo {",
            "   A a = new A();",
            "   B b = new B();",
            "   C c = new C();",
            "   D d = new D();",
            "   E e = new E();",
            "   F f = new F();",
            "   G g = new G();",
            "   H h = new H();",
            "   I i = new I();",
            "   J j = new J();",
            "   K k = new K();",
            "   L l = new L();",
            "   M m = new M();",
            "   N n = new N();",
            "   P p = new P();",
            "   Q q = new Q();",
            "   R r = new R();",
            "   S s = new S();",
            "   T t = new T();",
            "   U u = new U();",
            "   V v = new V();",
            "   W w = new W();",
            "   X x = new X();",
            "   Y y = new Y();",
            "   Z z = new Z();",
            "   AA aa = new AA();",
            "   AB ab = new AB();",
            "   Object test() {",
            "       return new Object();",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import static com.ibm.wala.types.A;",
            "import static com.ibm.wala.types.B;",
            "import static com.ibm.wala.types.C;",
            "import static com.ibm.wala.types.D;",
            "import static com.ibm.wala.types.E;",
            "import static com.ibm.wala.types.F;",
            "import static com.ibm.wala.types.G;",
            "import static com.ibm.wala.types.H;",
            "import static com.ibm.wala.types.I;",
            "import static com.ibm.wala.types.J;",
            "import static com.ibm.wala.types.K;",
            "import static com.ibm.wala.types.L;",
            "import com.ibm.wala.util.A;",
            "import com.ibm.wala.util.B;",
            "import com.ibm.wala.util.C;",
            "import com.ibm.wala.util.D;",
            "import com.ibm.wala.util.E;",
            "import com.ibm.wala.util.F;",
            "import com.ibm.wala.util.G;",
            "import com.ibm.wala.util.H;",
            "import com.ibm.wala.util.I;",
            "import com.ibm.wala.util.J;",
            "import com.ibm.wala.util.K;",
            "import com.ibm.wala.util.L;",
            "import com.ibm.wala.util.M;",
            "import com.ibm.wala.util.N;",
            "import com.ibm.wala.util.O;",
            "import com.ibm.wala.util.P;",
            "import com.ibm.wala.util.Q;",
            "import com.ibm.wala.util.R;",
            "import com.ibm.wala.util.S;",
            "import com.ibm.wala.util.T;",
            "import com.ibm.wala.util.U;",
            "import com.ibm.wala.util.V;",
            "import com.ibm.wala.util.W;",
            "import com.ibm.wala.util.X;",
            "import com.ibm.wala.util.Y;",
            "import com.ibm.wala.util.Z;",
            "import com.ibm.wala.util.AA;",
            "import com.ibm.wala.util.AB;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   A a = new A();",
            "   B b = new B();",
            "   C c = new C();",
            "   D d = new D();",
            "   E e = new E();",
            "   F f = new F();",
            "   G g = new G();",
            "   H h = new H();",
            "   I i = new I();",
            "   J j = new J();",
            "   K k = new K();",
            "   L l = new L();",
            "   M m = new M();",
            "   N n = new N();",
            "   P p = new P();",
            "   Q q = new Q();",
            "   R r = new R();",
            "   S s = new S();",
            "   T t = new T();",
            "   U u = new U();",
            "   V v = new V();",
            "   W w = new W();",
            "   X x = new X();",
            "   Y y = new Y();",
            "   Z z = new Z();",
            "   AA aa = new AA();",
            "   AB ab = new AB();",
            "   @Nullable Object test() {",
            "       return new Object();",
            "   }",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnMethod("Foo.java", "test.Foo", "test()"), "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void removeRedundantNewKeyword() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "public class Foo {",
            "   Object test() {",
            "       init(this.new NodeVisitor(), this.new EdgeVisitor());",
            "       return foo(this.new Bar(), this.new Foo(), getBuilder().new Foo());",
            "   }",
            "   Object foo(Bar b, Foo f) {",
            "     return Object();",
            "   }",
            "   class Foo{ }",
            "   class Bar{ }",
            "}")
        .expectOutput(
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   @Nullable Object test() {",
            "       init(this.new NodeVisitor(), this.new EdgeVisitor());",
            "       return foo(this.new Bar(), this.new Foo(), getBuilder().new Foo());",
            "   }",
            "   Object foo(Bar b, Foo f) {",
            "     return Object();",
            "   }",
            "   class Foo{ }",
            "   class Bar{ }",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnMethod("Foo.java", "test.Foo", "test()"), "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void simpleArrayBracketPreservation() {
    injectorTestHelper
        .addInput(
            "A.java", "package test;", "public class A {", "   private Object[] allTest;", "}")
        .expectOutput(
            "package test;",
            "import javax.annotation.Nullable;",
            "public class A {",
            "   private Object @Nullable [] allTest;",
            "}")
        .addInput(
            "B.java", "package test;", "public class B {", "   private Object allTest[];", "}")
        .expectOutput(
            "package test;",
            "import javax.annotation.Nullable;",
            "public class B {",
            "   @Nullable private Object allTest[];",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField("B.java", "test.B", Collections.singleton("allTest")),
                "javax.annotation.Nullable"),
            new AddMarkerAnnotation(
                new OnField("A.java", "test.A", Collections.singleton("allTest")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void methodInInterfaceWithTab() {
    injectorTestHelper
        .addInput(
            "Run.java",
            "package edu.ucr;",
            "public interface Run {",
            "   /**",
            "    * javadoc",
            "    */",
            "\tpublic Object run();",
            "}")
        .expectOutput(
            "package edu.ucr;",
            "import javax.annotation.Nullable;",
            "public interface Run {",
            "   /**",
            "    * javadoc",
            "    */",
            "\t@Nullable public Object run();",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnMethod("Run.java", "edu.ucr.Run", "run()"), "javax.annotation.Nullable"))
        .start();
  }
}
