/*
 * MIT License
 *
 * Copyright (c) 2023 Nima Karimipour
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
import edu.ucr.cs.riple.injector.changes.AddTypeUseMarkerAnnotation;
import edu.ucr.cs.riple.injector.changes.RemoveTypeUseAnnotation;
import edu.ucr.cs.riple.injector.location.OnLocalVariable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class OnLocalVariableInjectionTest extends BaseInjectorTest {

  @Test
  public void additionTest() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "public class Foo {",
            "   public void foo() {",
            "      int f0;",
            "      Bar<String, Integer, Baz<String, Integer>> f1;",
            "      String f2;",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.UnTainted;",
            "public class Foo {",
            "   public void foo() {",
            "      @UnTainted int f0;",
            "      @UnTainted Bar<@UnTainted String, @UnTainted Integer, @UnTainted Baz<@UnTainted String, @UnTainted Integer>> f1;",
            "      @UnTainted String f2;",
            "   }",
            "}")
        .addChanges(
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f0"), "edu.ucr.UnTainted"),
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f1"), "edu.ucr.UnTainted"),
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f2"), "edu.ucr.UnTainted"))
        .start();
  }

  @Test
  public void deletionTest() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import edu.ucr.UnTainted;",
            "public class Foo {",
            "   public void foo() {",
            "      @UnTainted int f0;",
            "      @UnTainted Bar<@UnTainted String, @UnTainted Integer, @UnTainted Baz<@UnTainted String, @UnTainted Integer>> f1;",
            "      @UnTainted String f2;",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.UnTainted;",
            "public class Foo {",
            "   public void foo() {",
            "      int f0;",
            "      Bar<String, Integer, Baz<String, Integer>> f1;",
            "      String f2;",
            "   }",
            "}")
        .addChanges(
            new RemoveTypeUseAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f0"), "edu.ucr.UnTainted"),
            new RemoveTypeUseAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f1"), "edu.ucr.UnTainted"),
            new RemoveTypeUseAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f2"), "edu.ucr.UnTainted"))
        .start();
  }

  @Test
  public void onLocalVariableWithIdenticalExistingInnerClassField() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import edu.ucr.UnTainted;",
            "public class Foo {",
            "   public void foo() {",
            "       class InnerFoo {",
            "          Object f0;",
            "       }",
            "       Object f0;",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.UnTainted;",
            "public class Foo {",
            "   public void foo() {",
            "       class InnerFoo {",
            "          Object f0;",
            "       }",
            "       @UnTainted Object f0;",
            "   }",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f0"), "edu.ucr.UnTainted"))
        .start();
  }

  @Test
  public void onLocalVariableWithIdenticalExistingInnerMethodLocalVariable() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import edu.ucr.UnTainted;",
            "public class Foo {",
            "   public void foo() {",
            "       class InnerFoo {",
            "          public void foo() {",
            "              Object f0;",
            "          }",
            "       }",
            "       Object f0;",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.UnTainted;",
            "public class Foo {",
            "   public void foo() {",
            "       class InnerFoo {",
            "          public void foo() {",
            "              Object f0;",
            "          }",
            "       }",
            "       @UnTainted Object f0;",
            "   }",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f0"), "edu.ucr.UnTainted"))
        .start();
  }

  @Test
  public void onArrayType() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "public class Foo<T> {",
            "   public void foo() {",
            "      Map<String, T[]> f0;",
            "      T[] f1;",
            "      String[] f2;",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.UnTainted;",
            "public class Foo<T> {",
            "   public void foo() {",
            "      @UnTainted Map<@UnTainted String, @UnTainted T[]> f0;",
            "      @UnTainted T[] f1;",
            "      @UnTainted String[] f2;",
            "   }",
            "}")
        .addChanges(
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f0"), "edu.ucr.UnTainted"),
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f1"), "edu.ucr.UnTainted"),
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f2"), "edu.ucr.UnTainted"))
        .start();
  }
}
