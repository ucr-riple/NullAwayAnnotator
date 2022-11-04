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
  public void save_imports() {
    injectorTestHelper
        .addInput(
            "Super.java",
            "package com.uber;",
            "import static com.ibm.wala.types.TypeName.ArrayMask;",
            "import static com.ibm.wala.types.TypeName.ElementBits;",
            "import static com.ibm.wala.types.TypeName.PrimitiveMask;",
            "import com.ibm.wala.types.TypeName.IntegerMask;",
            "import com.ibm.wala.util.collections.HashMapFactory;",
            "import java.io.Serializable;",
            "import java.util.Map;",
            "public class Super {",
            "   Object test() {",
            "       return new Object();",
            "   }",
            "}")
        .expectOutput(
            "package com.uber;",
            "import static com.ibm.wala.types.TypeName.ArrayMask;",
            "import static com.ibm.wala.types.TypeName.ElementBits;",
            "import static com.ibm.wala.types.TypeName.PrimitiveMask;",
            "import com.ibm.wala.types.TypeName.IntegerMask;",
            "import com.ibm.wala.util.collections.HashMapFactory;",
            "import java.io.Serializable;",
            "import java.util.Map;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable",
            "   Object test() {",
            "       return new Object();",
            "   }",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnMethod("Super.java", "com.uber.Super", "test()"),
                "javax.annotation.Nullable"))
        .start(true);
  }

  @Test
  public void save_imports_asterisk() {
    injectorTestHelper
        .addInput(
            "Super.java",
            "package com.uber;",
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
            "public class Super {",
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
            "package com.uber;",
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
            "public class Super {",
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
            "   @Nullable",
            "   Object test() {",
            "       return new Object();",
            "   }",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnMethod("Super.java", "com.uber.Super", "test()"),
                "javax.annotation.Nullable"))
        .start(true);
  }

  @Test
  public void remove_redundant_new_keyword() {
    injectorTestHelper
        .addInput(
            "Super.java",
            "package com.uber;",
            "public class Super {",
            "   Object test() {",
            "       init(this.new NodeVisitor(), this.new EdgeVisitor());\n",
            "       return foo(this.new Bar(), this.new Foo(), getBuilder().new Foo());",
            "   }",
            "   Object foo(Bar b, Foo f) {",
            "     return Object();",
            "   }",
            "   class Foo{ }",
            "   class Bar{ }",
            "}")
        .expectOutput(
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable",
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
        .addChanges(
            new AddMarkerAnnotation(
                new OnMethod("Super.java", "com.uber.Super", "test()"),
                "javax.annotation.Nullable"))
        .start(true);
  }

  @Test
  public void simple_array_bracket_preservation() {
    injectorTestHelper
        .addInput(
            "A.java", "package com.uber;", "public class A {", "   private Object[] allTest;", "}")
        .expectOutput(
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class A {",
            "   @Nullable",
            "   private Object[] allTest;",
            "}")
        .addInput(
            "B.java", "package com.uber;", "public class B {", "   private Object allTest[];", "}")
        .expectOutput(
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class B {",
            "   @Nullable",
            "   private Object allTest[];",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField("B.java", "com.uber.B", Collections.singleton("allTest")),
                "javax.annotation.Nullable"),
            new AddMarkerAnnotation(
                new OnField("A.java", "com.uber.A", Collections.singleton("allTest")),
                "javax.annotation.Nullable"))
        .start(true);
  }
}
