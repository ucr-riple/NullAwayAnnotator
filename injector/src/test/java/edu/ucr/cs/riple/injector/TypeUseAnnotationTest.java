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

import edu.ucr.cs.riple.injector.changes.AddFullTypeMarkerAnnotation;
import edu.ucr.cs.riple.injector.changes.RemoveFullTypeMarkerAnnotation;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.injector.location.OnLocalVariable;
import edu.ucr.cs.riple.injector.location.OnMethod;
import edu.ucr.cs.riple.injector.location.OnParameter;
import java.util.Set;
import org.junit.Test;

public class TypeUseAnnotationTest extends BaseInjectorTest {

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
            new AddFullTypeMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f0"), "edu.ucr.UnTainted"),
            new AddFullTypeMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f1"), "edu.ucr.UnTainted"),
            new AddFullTypeMarkerAnnotation(
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
            new RemoveFullTypeMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f0"), "edu.ucr.UnTainted"),
            new RemoveFullTypeMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f1"), "edu.ucr.UnTainted"),
            new RemoveFullTypeMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f2"), "edu.ucr.UnTainted"))
        .start();
  }

  @Test
  public void additionOnFullyQualifiedTypeNamesTest() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "public class Foo {",
            "   java.lang.Object bar;",
            "   java.util.Map<java.lang.String, String[]> f0;",
            "   java.lang.Object baz(java.lang.Object param) {;",
            "       java.lang.Object localVar;",
            "       return new Object();",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import custom.example.Untainted;",
            "public class Foo {",
            "   java.lang.@Untainted Object bar;",
            "   java.util.@Untainted Map<java.lang.@Untainted String, @Untainted String[]> f0;",
            "   java.lang.@Untainted Object baz(java.lang.@Untainted Object param) {;",
            "       java.lang.@Untainted Object localVar;",
            "       return new Object();",
            "   }",
            "}")
        .addChanges(
            new AddFullTypeMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Set.of("bar")), "custom.example.Untainted"),
            new AddFullTypeMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Set.of("f0")), "custom.example.Untainted"),
            new AddFullTypeMarkerAnnotation(
                new OnMethod("Foo.java", "test.Foo", "baz(java.lang.Object)"),
                "custom.example.Untainted"),
            new AddFullTypeMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "baz(java.lang.Object)", "localVar"),
                "custom.example.Untainted"),
            new AddFullTypeMarkerAnnotation(
                new OnParameter("Foo.java", "test.Foo", "baz(java.lang.Object)", 0),
                "custom.example.Untainted"))
        .start();
  }

  @Test
  public void deletionOnFullyQualifiedTypeNamesTest() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import custom.example.Untainted;",
            "public class Foo {",
            "   java.lang.@Untainted Object bar;",
            "   java.util.@Untainted Map<java.lang.@Untainted String, @Untainted String[]> f0;",
            "   java.lang.@Untainted Object baz(java.lang.@Untainted Object param) {;",
            "       java.lang.@Untainted Object localVar;",
            "       return new Object();",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import custom.example.Untainted;",
            "public class Foo {",
            "   java.lang.Object bar;",
            "   java.util.Map<java.lang.String, String[]> f0;",
            "   java.lang.Object baz(java.lang.Object param) {;",
            "       java.lang.Object localVar;",
            "       return new Object();",
            "   }",
            "}")
        .addChanges(
            new RemoveFullTypeMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Set.of("bar")), "custom.example.Untainted"),
            new RemoveFullTypeMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Set.of("f0")), "custom.example.Untainted"),
            new RemoveFullTypeMarkerAnnotation(
                new OnMethod("Foo.java", "test.Foo", "baz(java.lang.Object)"),
                "custom.example.Untainted"),
            new RemoveFullTypeMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "baz(java.lang.Object)", "localVar"),
                "custom.example.Untainted"),
            new RemoveFullTypeMarkerAnnotation(
                new OnParameter("Foo.java", "test.Foo", "baz(java.lang.Object)", 0),
                "custom.example.Untainted"))
        .start();
  }

  @Test
  public void additionOnArrayTest() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "public class Foo<T> {",
            "   public void foo() {",
            "      java.util.Map<java.lang.String, String[]> f0;",
            "      Map<T, T>[] f1;",
            "      String[] f2;",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.UnTainted;",
            "public class Foo<T> {",
            "   public void foo() {",
            "      java.util.@UnTainted Map<java.lang.@UnTainted String, @UnTainted String[]> f0;",
            "      @UnTainted Map<@UnTainted T, @UnTainted T>[] f1;",
            "      @UnTainted String[] f2;",
            "   }",
            "}")
        .addChanges(
            new AddFullTypeMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f0"), "edu.ucr.UnTainted"),
            new AddFullTypeMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f1"), "edu.ucr.UnTainted"),
            new AddFullTypeMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f2"), "edu.ucr.UnTainted"))
        .start();
  }

  @Test
  public void deletionOnArrayTest() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import custom.example.Untainted;",
            "public class Foo {",
            "   java.util.Map<String, String[]> f0;",
            "   java.util.@Untainted Map<@Untainted String, @Untainted String[]> f1;",
            "   @Untainted Map<java.util.@Untainted Map, @Untainted String>[] f2;",
            "}")
        .expectOutput(
            "package test;",
            "import custom.example.Untainted;",
            "public class Foo {",
            "   java.util.@Untainted Map<@Untainted String, @Untainted String[]> f0;",
            "   java.util.Map<String, String[]> f1;",
            "   Map<java.util.Map, String>[] f2;",
            "}")
        .addChanges(
            new AddFullTypeMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Set.of("f0")), "custom.example.Untainted"),
            new RemoveFullTypeMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Set.of("f1")), "custom.example.Untainted"),
            new RemoveFullTypeMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Set.of("f2")), "custom.example.Untainted"))
        .start();
  }

  @Test
  public void additionOnInitializerTest() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "public class Foo {",
            "   public void foo() {",
            "      int f0 = 0;",
            "      Bar<String, Integer, Baz<String, Integer>> f1 = new Custom<String, String>();",
            "      String f2 = \"FOO\";",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.UnTainted;",
            "public class Foo {",
            "   public void foo() {",
            "      @UnTainted int f0 = 0;",
            "      @UnTainted Bar<@UnTainted String, @UnTainted Integer, @UnTainted Baz<@UnTainted String, @UnTainted Integer>> f1 = new Custom<@UnTainted String, @UnTainted String>();",
            "      @UnTainted String f2 = \"FOO\";",
            "   }",
            "}")
        .addChanges(
            new AddFullTypeMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f0"), "edu.ucr.UnTainted"),
            new AddFullTypeMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f1"), "edu.ucr.UnTainted"),
            new AddFullTypeMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f2"), "edu.ucr.UnTainted"))
        .start();
  }

  @Test
  public void quick() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import edu.ucr.UnTainted;",
            "public class Foo {",
            "   public void foo() {",
            "      @UnTainted int f0 = 0;",
            "      @UnTainted Bar<@UnTainted String, @UnTainted Integer, @UnTainted Baz<@UnTainted String, @UnTainted Integer>> f1 = new Custom<@UnTainted String, @UnTainted String>();",
            "      @UnTainted String f2 = \"FOO\";",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.UnTainted;",
            "public class Foo {",
            "   public void foo() {",
            "      int f0 = 0;",
            "      Bar<@UnTainted String, @UnTainted Integer, @UnTainted Baz<@UnTainted String, @UnTainted Integer>> f1 = new Custom<@UnTainted String, @UnTainted String>();",
            "      String f2 = \"FOO\";",
            "   }",
            "}")
        .addChanges(
            new AddFullTypeMarkerAnnotation(
                    new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f0"), "edu.ucr.UnTainted")
                .toDeclaration()
                .getReverse(),
            new AddFullTypeMarkerAnnotation(
                    new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f1"), "edu.ucr.UnTainted")
                .toDeclaration()
                .getReverse(),
            new AddFullTypeMarkerAnnotation(
                    new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f2"), "edu.ucr.UnTainted")
                .toDeclaration()
                .getReverse())
        .start();
  }

  @Test
  public void avoidDuplicate() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import edu.ucr.RUntainted;",
            "public class Foo {",
            "   public @RUntainted Map<@RUntainted String, @RUntainted String> bar();",
            "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.RUntainted;",
            "public class Foo {",
            "   public @RUntainted Map<@RUntainted String, @RUntainted String> bar();",
            "}")
        .addChanges(
            new AddFullTypeMarkerAnnotation(
                new OnMethod("Foo.java", "test.Foo", "bar()"), "edu.ucr.RUntainted"))
        .start();
  }
}
