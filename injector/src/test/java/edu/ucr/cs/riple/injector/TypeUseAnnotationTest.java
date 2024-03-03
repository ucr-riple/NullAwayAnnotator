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

import com.google.common.collect.ImmutableList;
import edu.ucr.cs.riple.injector.changes.AddMarkerAnnotation;
import edu.ucr.cs.riple.injector.changes.AddTypeUseMarkerAnnotation;
import edu.ucr.cs.riple.injector.changes.RemoveTypeUseMarkerAnnotation;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.injector.location.OnLocalVariable;
import edu.ucr.cs.riple.injector.location.OnMethod;
import edu.ucr.cs.riple.injector.location.OnParameter;
import java.util.Collections;
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
            "      @UnTainted Bar<@UnTainted String, Integer, @UnTainted Baz<String, @UnTainted Integer>> f1;",
            "      @UnTainted String f2;",
            "   }",
            "}")
        .addChanges(
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f0"), "edu.ucr.UnTainted"),
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f1"),
                "edu.ucr.UnTainted",
                ImmutableList.of(
                    ImmutableList.of(0),
                    ImmutableList.of(1, 0),
                    ImmutableList.of(3, 0),
                    ImmutableList.of(3, 2, 0))),
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
            "      @UnTainted Bar<@UnTainted String, Integer, @UnTainted Baz<String, @UnTainted Integer>> f1;",
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
            new RemoveTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f0"), "edu.ucr.UnTainted"),
            new RemoveTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f1"),
                "edu.ucr.UnTainted",
                ImmutableList.of(
                    ImmutableList.of(0),
                    ImmutableList.of(1, 0),
                    ImmutableList.of(3, 0),
                    ImmutableList.of(3, 2, 0))),
            new RemoveTypeUseMarkerAnnotation(
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
            new AddTypeUseMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Set.of("bar")), "custom.example.Untainted"),
            new AddTypeUseMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Set.of("f0")),
                "custom.example.Untainted",
                ImmutableList.of(
                    ImmutableList.of(0), ImmutableList.of(1, 0), ImmutableList.of(2, 0))),
            new AddTypeUseMarkerAnnotation(
                new OnMethod("Foo.java", "test.Foo", "baz(java.lang.Object)"),
                "custom.example.Untainted"),
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "baz(java.lang.Object)", "localVar"),
                "custom.example.Untainted"),
            new AddTypeUseMarkerAnnotation(
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
            new RemoveTypeUseMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Set.of("bar")), "custom.example.Untainted"),
            new RemoveTypeUseMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Set.of("f0")),
                "custom.example.Untainted",
                ImmutableList.of(
                    ImmutableList.of(0), ImmutableList.of(1, 0), ImmutableList.of(2, 0))),
            new RemoveTypeUseMarkerAnnotation(
                new OnMethod("Foo.java", "test.Foo", "baz(java.lang.Object)"),
                "custom.example.Untainted"),
            new RemoveTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "baz(java.lang.Object)", "localVar"),
                "custom.example.Untainted"),
            new RemoveTypeUseMarkerAnnotation(
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
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f0"),
                "edu.ucr.UnTainted",
                ImmutableList.of(
                    ImmutableList.of(0), ImmutableList.of(1, 0), ImmutableList.of(2, 0))),
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f1"),
                "edu.ucr.UnTainted",
                ImmutableList.of(
                    ImmutableList.of(0), ImmutableList.of(1, 0), ImmutableList.of(2, 0))),
            new AddTypeUseMarkerAnnotation(
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
            new AddTypeUseMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Set.of("f0")),
                "custom.example.Untainted",
                ImmutableList.of(
                    ImmutableList.of(0), ImmutableList.of(1, 0), ImmutableList.of(2, 0))),
            new RemoveTypeUseMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Set.of("f1")),
                "custom.example.Untainted",
                ImmutableList.of(
                    ImmutableList.of(0), ImmutableList.of(1, 0), ImmutableList.of(2, 0))),
            new RemoveTypeUseMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Set.of("f2")),
                "custom.example.Untainted",
                ImmutableList.of(
                    ImmutableList.of(0), ImmutableList.of(1, 0), ImmutableList.of(2, 0))))
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
            "      Bar<String, Integer, Baz<String, Integer>> f1 = new Bar<String, Integer, Baz<String, Integer>>();",
            "      String f2 = \"FOO\";",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.UnTainted;",
            "public class Foo {",
            "   public void foo() {",
            "      @UnTainted int f0 = 0;",
            "      @UnTainted Bar<@UnTainted String, Integer, Baz<String, @UnTainted Integer>> f1 = new Bar<@UnTainted String, Integer, Baz<String, @UnTainted Integer>>();",
            "      @UnTainted String f2 = \"FOO\";",
            "   }",
            "}")
        .addChanges(
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f0"), "edu.ucr.UnTainted"),
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f1"),
                "edu.ucr.UnTainted",
                ImmutableList.of(
                    ImmutableList.of(0), ImmutableList.of(1, 0), ImmutableList.of(3, 2, 0))),
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f2"), "edu.ucr.UnTainted"))
        .start();
  }

  @Test
  public void removalOnDeclarationOnly() {
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
            new AddTypeUseMarkerAnnotation(
                    new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f0"), "edu.ucr.UnTainted")
                .toDeclaration()
                .getReverse(),
            new AddTypeUseMarkerAnnotation(
                    new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f1"), "edu.ucr.UnTainted")
                .toDeclaration()
                .getReverse(),
            new AddTypeUseMarkerAnnotation(
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
            new AddTypeUseMarkerAnnotation(
                new OnMethod("Foo.java", "test.Foo", "bar()"),
                "edu.ucr.RUntainted",
                ImmutableList.of(
                    ImmutableList.of(0), ImmutableList.of(1, 0), ImmutableList.of(2, 0))))
        .start();
  }

  @Test
  public void deletionOnInitializerTest() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import edu.ucr.UnTainted;",
            "public class Foo {",
            "   public void foo() {",
            "      @UnTainted int f0 = 0;",
            "      @UnTainted Bar<@UnTainted String, @UnTainted Integer, @UnTainted Baz<@UnTainted String, @UnTainted Integer>> f1 = new Bar<@UnTainted String, @UnTainted Integer, @UnTainted Baz<@UnTainted String, @UnTainted Integer>>();",
            "      @UnTainted String f2 = \"FOO\";",
            "      @UnTainted Bar<@UnTainted String, @UnTainted Integer[], @UnTainted Baz<@UnTainted String, @UnTainted Integer>> f3 = new Bar<@UnTainted String, @UnTainted Integer[], @UnTainted Baz<@UnTainted String, @UnTainted Integer>>();",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.UnTainted;",
            "public class Foo {",
            "   public void foo() {",
            "      int f0 = 0;",
            "      Bar<String, Integer, Baz<String, Integer>> f1 = new Bar<String, Integer, Baz<String, Integer>>();",
            "      String f2 = \"FOO\";",
            "      Bar<String, Integer[], Baz<String, Integer>> f3 = new Bar<String, Integer[], Baz<String, Integer>>();",
            "   }",
            "}")
        .addChanges(
            new RemoveTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f0"), "edu.ucr.UnTainted"),
            new RemoveTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f1"),
                "edu.ucr.UnTainted",
                ImmutableList.of(
                    ImmutableList.of(0),
                    ImmutableList.of(1, 0),
                    ImmutableList.of(2, 0),
                    ImmutableList.of(3, 0),
                    ImmutableList.of(3, 1, 0),
                    ImmutableList.of(3, 2, 0))),
            new RemoveTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f2"), "edu.ucr.UnTainted"),
            new RemoveTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f3"),
                "edu.ucr.UnTainted",
                ImmutableList.of(
                    ImmutableList.of(0),
                    ImmutableList.of(1, 0),
                    ImmutableList.of(2, 0),
                    ImmutableList.of(3, 0),
                    ImmutableList.of(3, 1, 0),
                    ImmutableList.of(3, 2, 0))))
        .start();
  }

  @Test
  public void avoidDuplicateOnAnnotationOnInnerClass() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import edu.ucr.Untainted;",
            "public class Foo {",
            "   public void foo() {",
            "     for (Map.@Untainted Entry<@Untainted String, @Untainted String> entry : m_additionalPreferences.entrySet()) {",
            "       String key = entry.getKey();",
            "       @RUntainted String value = entry.getValue();",
            "       m_user.setAdditionalInfo(PREFERENCES_ADDITIONAL_PREFIX + key, value);",
            "     }",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.Untainted;",
            "public class Foo {",
            "   public void foo() {",
            "     for (Map.@Untainted Entry<@Untainted String, @Untainted String> entry : m_additionalPreferences.entrySet()) {",
            "       String key = entry.getKey();",
            "       @RUntainted String value = entry.getValue();",
            "       m_user.setAdditionalInfo(PREFERENCES_ADDITIONAL_PREFIX + key, value);",
            "     }",
            "   }",
            "}")
        .addChanges(
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "entry"), "edu.ucr.Untainted"))
        .start();
  }

  @Test
  public void addTypeUseOnType() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import edu.ucr.Untainted;",
            "public class Foo {",
            "   private final Object f0;",
            "   private final Object f1;",
            "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.Untainted;",
            "public class Foo {",
            "   @Untainted private final Object f0;",
            "   private final @Untainted Object f1;",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Collections.singleton("f0")),
                "edu.ucr.Untainted"),
            new AddTypeUseMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Collections.singleton("f1")),
                "edu.ucr.Untainted"))
        .start();
  }

  @Test
  public void additionOnFullyQualifiedTypeNamesOnTypeArgsTest() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "public class Foo {",
            "   void baz(java.lang.Object param) {",
            "       final java.util.HashMap<java.lang.String,java.lang.String> localVar = new HashMap<String, String>();",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import custom.example.Untainted;",
            "public class Foo {",
            "   void baz(java.lang.Object param) {",
            "       final java.util.HashMap<java.lang.@Untainted String,java.lang.@Untainted String> localVar = new HashMap<@Untainted String, @Untainted String>();",
            "   }",
            "}")
        .addChanges(
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "baz(java.lang.Object)", "localVar"),
                "custom.example.Untainted",
                ImmutableList.of(ImmutableList.of(1, 0))),
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "baz(java.lang.Object)", "localVar"),
                "custom.example.Untainted",
                ImmutableList.of(ImmutableList.of(2, 0))))
        .start();
  }

  @Test
  public void deletionOnFullyQualifiedTypeNamesOnTypeArgsTest() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import custom.example.Untainted;",
            "public class Foo {",
            "   void baz(java.lang.Object param) {",
            "       final java.util.HashMap<java.lang.@Untainted String,java.lang.@Untainted String> localVar = new HashMap<@Untainted String, @Untainted String>();",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import custom.example.Untainted;",
            "public class Foo {",
            "   void baz(java.lang.Object param) {",
            "       final java.util.HashMap<java.lang.String,java.lang.String> localVar = new HashMap<String, String>();",
            "   }",
            "}")
        .addChanges(
            new RemoveTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "baz(java.lang.Object)", "localVar"),
                "custom.example.Untainted",
                ImmutableList.of(ImmutableList.of(1, 0))),
            new RemoveTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "baz(java.lang.Object)", "localVar"),
                "custom.example.Untainted",
                ImmutableList.of(ImmutableList.of(2, 0))))
        .start();
  }

  @Test
  public void onArrayTypeDuplicateTest() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import custom.example.Untainted;",
            "public class Foo {",
            "   void baz(java.lang.Object param) {",
            "       final java.lang.@RUntainted String[] localVar = content.split(\"\\n\");",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import custom.example.Untainted;",
            "public class Foo {",
            "   void baz(java.lang.Object param) {",
            "       final java.lang.@RUntainted String[] localVar = content.split(\"\\n\");",
            "   }",
            "}")
        .addChanges(
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "baz(java.lang.Object)", "localVar"),
                "custom.example.RUntainted"),
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "baz(java.lang.Object)", "localVar"),
                "custom.example.RUntainted"),
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "baz(java.lang.Object)", "localVar"),
                "custom.example.RUntainted"),
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "baz(java.lang.Object)", "localVar"),
                "custom.example.RUntainted"))
        .start();
  }

  @Test
  public void onArrayTypeDeletionTest() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import custom.example.Untainted;",
            "public class Foo {",
            "   void baz(java.lang.Object param) {",
            "       final java.lang.@RUntainted String[] localVar = content.split(\"\\n\");",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import custom.example.Untainted;",
            "public class Foo {",
            "   void baz(java.lang.Object param) {",
            "       final java.lang.String[] localVar = content.split(\"\\n\");",
            "   }",
            "}")
        .addChanges(
            new RemoveTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "baz(java.lang.Object)", "localVar"),
                "custom.example.RUntainted"))
        .start();
  }

  @Test
  public void additionOnInitializerFieldTest() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "public class Foo {",
            "   Map<String, String> map = new HashMap<String, String>();",
            "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.UnTainted;",
            "public class Foo {",
            "   Map<String, @UnTainted String> map = new HashMap<String, @UnTainted String>();",
            "}")
        .addChanges(
            new AddTypeUseMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Collections.singleton("map")),
                "edu.ucr.UnTainted",
                ImmutableList.of(ImmutableList.of(2, 0))))
        .start();
  }

  @Test
  public void deletionOnInitializerFieldTest() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import edu.ucr.UnTainted;",
            "public class Foo {",
            "   Map<String, @UnTainted String> map = new HashMap<String, @UnTainted String>();",
            "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.UnTainted;",
            "public class Foo {",
            "   Map<String, String> map = new HashMap<String, String>();",
            "}")
        .addChanges(
            new RemoveTypeUseMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Collections.singleton("map")),
                "edu.ucr.UnTainted",
                ImmutableList.of(ImmutableList.of(2, 0))))
        .start();
  }

  @Test
  public void additionOnWildCard() {
    injectorTestHelper
        .addInput("Foo.java", "package test;", "public class Foo {", "   Map<String, ?> map;", "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.UnTainted;",
            "public class Foo {",
            "   Map<String, @UnTainted ?> map;",
            "}")
        .addChanges(
            new AddTypeUseMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Collections.singleton("map")),
                "edu.ucr.UnTainted",
                ImmutableList.of(ImmutableList.of(2, 0))))
        .start();
  }

  @Test
  public void deletionOnWildCard() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import edu.ucr.UnTainted;",
            "public class Foo {",
            "   Map<String, @UnTainted ?> map;",
            "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.UnTainted;",
            "public class Foo {",
            "   Map<String, ?> map;",
            "}")
        .addChanges(
            new RemoveTypeUseMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Collections.singleton("map")),
                "edu.ucr.UnTainted",
                ImmutableList.of(ImmutableList.of(2, 0))))
        .start();
  }

  @Test
  public void wildCardExtendedType() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import edu.ucr.UnTainted;",
            "public class Foo {",
            "   public synchronized ConfigurationBuilder<? extends HierarchicalConfiguration<?>> m() throws ConfigurationException {}",
            "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.UnTainted;",
            "public class Foo {",
            "   public synchronized ConfigurationBuilder<? extends @UnTainted HierarchicalConfiguration<?>> m() throws ConfigurationException {}",
            "}")
        .addChanges(
            new AddTypeUseMarkerAnnotation(
                new OnMethod("Foo.java", "test.Foo", "m()"),
                "edu.ucr.UnTainted",
                ImmutableList.of(ImmutableList.of(1, 0))))
        .start();
  }

  @Test
  public void existingWildCardExtendedType() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import edu.ucr.UnTainted;",
            "public class Foo {",
            "   public synchronized ConfigurationBuilder<? extends @UnTainted HierarchicalConfiguration<?>> m() throws ConfigurationException {}",
            "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.UnTainted;",
            "public class Foo {",
            "   public synchronized ConfigurationBuilder<? extends @UnTainted HierarchicalConfiguration<?>> m() throws ConfigurationException {}",
            "}")
        .addChanges(
            new AddTypeUseMarkerAnnotation(
                new OnMethod("Foo.java", "test.Foo", "m()"),
                "edu.ucr.UnTainted",
                ImmutableList.of(ImmutableList.of(1, 0))))
        .start();
  }

  @Test
  public void removeWildCardExtendedType() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import edu.ucr.UnTainted;",
            "public class Foo {",
            "   public synchronized ConfigurationBuilder<? extends @UnTainted HierarchicalConfiguration<?>> m() throws ConfigurationException {}",
            "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.UnTainted;",
            "public class Foo {",
            "   public synchronized ConfigurationBuilder<? extends HierarchicalConfiguration<?>> m() throws ConfigurationException {}",
            "}")
        .addChanges(
            new RemoveTypeUseMarkerAnnotation(
                new OnMethod("Foo.java", "test.Foo", "m()"),
                "edu.ucr.UnTainted",
                ImmutableList.of(ImmutableList.of(1, 0))))
        .start();
  }
}

// public synchronized ConfigurationBuilder<? extends HierarchicalConfiguration<?>>
// getDefinitionBuilder() throws ConfigurationException {
