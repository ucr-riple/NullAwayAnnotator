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
            "import edu.ucr.custom.Nullable;",
            "public class Foo {",
            "   public void foo() {",
            "      @Nullable int f0;",
            "      @Nullable Bar<@Nullable String, Integer, @Nullable Baz<String, @Nullable Integer>> f1;",
            "      @Nullable String f2;",
            "   }",
            "}")
        .addChanges(
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f0"),
                "edu.ucr.custom.Nullable"),
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f1"),
                "edu.ucr.custom.Nullable",
                ImmutableList.of(
                    ImmutableList.of(0),
                    ImmutableList.of(1, 0),
                    ImmutableList.of(3, 0),
                    ImmutableList.of(3, 2, 0))),
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f2"),
                "edu.ucr.custom.Nullable"))
        .start();
  }

  @Test
  public void deletionTest() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import edu.ucr.custom.Nullable;",
            "public class Foo {",
            "   public void foo() {",
            "      @Nullable int f0;",
            "      @Nullable Bar<@Nullable String, Integer, @Nullable Baz<String, @Nullable Integer>> f1;",
            "      @Nullable String f2;",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.custom.Nullable;",
            "public class Foo {",
            "   public void foo() {",
            "      int f0;",
            "      Bar<String, Integer, Baz<String, Integer>> f1;",
            "      String f2;",
            "   }",
            "}")
        .addChanges(
            new RemoveTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f0"),
                "edu.ucr.custom.Nullable"),
            new RemoveTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f1"),
                "edu.ucr.custom.Nullable",
                ImmutableList.of(
                    ImmutableList.of(0),
                    ImmutableList.of(1, 0),
                    ImmutableList.of(3, 0),
                    ImmutableList.of(3, 2, 0))),
            new RemoveTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f2"),
                "edu.ucr.custom.Nullable"))
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
            "import edu.ucr.custom.Nullable;",
            "public class Foo {",
            "   java.lang.@Nullable Object bar;",
            "   java.util.@Nullable Map<java.lang.@Nullable String, String@Nullable []> f0;",
            "   java.lang.@Nullable Object baz(java.lang.@Nullable Object param) {;",
            "       java.lang.@Nullable Object localVar;",
            "       return new Object();",
            "   }",
            "}")
        .addChanges(
            new AddTypeUseMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Set.of("bar")), "edu.ucr.custom.Nullable"),
            new AddTypeUseMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Set.of("f0")),
                "edu.ucr.custom.Nullable",
                ImmutableList.of(
                    ImmutableList.of(0), ImmutableList.of(1, 0), ImmutableList.of(2, 0))),
            new AddTypeUseMarkerAnnotation(
                new OnMethod("Foo.java", "test.Foo", "baz(java.lang.Object)"),
                "edu.ucr.custom.Nullable"),
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "baz(java.lang.Object)", "localVar"),
                "edu.ucr.custom.Nullable"),
            new AddTypeUseMarkerAnnotation(
                new OnParameter("Foo.java", "test.Foo", "baz(java.lang.Object)", 0),
                "edu.ucr.custom.Nullable"))
        .start();
  }

  @Test
  public void deletionOnFullyQualifiedTypeNamesTest() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import edu.ucr.custom.Nullable;",
            "public class Foo {",
            "   java.lang.@Nullable Object bar;",
            "   java.util.@Nullable Map<java.lang.@Nullable String, String @Nullable []> f0;",
            "   java.lang.@Nullable Object baz(java.lang.@Nullable Object param) {;",
            "       java.lang.@Nullable Object localVar;",
            "       return new Object();",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.custom.Nullable;",
            "public class Foo {",
            "   java.lang.Object bar;",
            "   java.util.Map<java.lang.String, String []> f0;",
            "   java.lang.Object baz(java.lang.Object param) {;",
            "       java.lang.Object localVar;",
            "       return new Object();",
            "   }",
            "}")
        .addChanges(
            new RemoveTypeUseMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Set.of("bar")), "edu.ucr.custom.Nullable"),
            new RemoveTypeUseMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Set.of("f0")),
                "edu.ucr.custom.Nullable",
                ImmutableList.of(
                    ImmutableList.of(0), ImmutableList.of(1, 0), ImmutableList.of(2, 0))),
            new RemoveTypeUseMarkerAnnotation(
                new OnMethod("Foo.java", "test.Foo", "baz(java.lang.Object)"),
                "edu.ucr.custom.Nullable"),
            new RemoveTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "baz(java.lang.Object)", "localVar"),
                "edu.ucr.custom.Nullable"),
            new RemoveTypeUseMarkerAnnotation(
                new OnParameter("Foo.java", "test.Foo", "baz(java.lang.Object)", 0),
                "edu.ucr.custom.Nullable"))
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
            "      int[] f2;",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.custom.Nullable;",
            "public class Foo<T> {",
            "   public void foo() {",
            "      java.util.@Nullable Map<java.lang.@Nullable String, String@Nullable []> f0;",
            "      Map<@Nullable T, @Nullable T>@Nullable [] f1;",
            "      int@Nullable [] f2;",
            "   }",
            "}")
        .addChanges(
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f0"),
                "edu.ucr.custom.Nullable",
                ImmutableList.of(
                    ImmutableList.of(0), ImmutableList.of(1, 0), ImmutableList.of(2, 0))),
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f1"),
                "edu.ucr.custom.Nullable",
                ImmutableList.of(
                    ImmutableList.of(0), ImmutableList.of(1, 1, 0), ImmutableList.of(1, 2, 0))),
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f2"),
                "edu.ucr.custom.Nullable"))
        .start();
  }

  @Test
  public void deletionOnArrayTest() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import edu.ucr.custom.Nullable;",
            "public class Foo {",
            "   java.util.Map<String, String[]> f0;",
            "   java.util.@Nullable Map<@Nullable String, String@Nullable []> f1;",
            "   Map<java.util.@Nullable Map, @Nullable String>@Nullable [] f2;",
            "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.custom.Nullable;",
            "public class Foo {",
            "   java.util.@Nullable Map<@Nullable String, String@Nullable []> f0;",
            "   java.util.Map<String, String[]> f1;",
            "   Map<java.util.Map, String>[] f2;",
            "}")
        .addChanges(
            new AddTypeUseMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Set.of("f0")),
                "edu.ucr.custom.Nullable",
                ImmutableList.of(
                    ImmutableList.of(0), ImmutableList.of(1, 0), ImmutableList.of(2, 0))),
            new RemoveTypeUseMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Set.of("f1")),
                "edu.ucr.custom.Nullable",
                ImmutableList.of(
                    ImmutableList.of(0), ImmutableList.of(1, 0), ImmutableList.of(2, 0))),
            new RemoveTypeUseMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Set.of("f2")),
                "edu.ucr.custom.Nullable",
                ImmutableList.of(
                    ImmutableList.of(0), ImmutableList.of(1, 1, 0), ImmutableList.of(1, 2, 0))))
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
            "import edu.ucr.custom.Nullable;",
            "public class Foo {",
            "   public void foo() {",
            "      @Nullable int f0 = 0;",
            "      @Nullable Bar<@Nullable String, Integer, Baz<String, @Nullable Integer>> f1 = new Bar<@Nullable String, Integer, Baz<String, @Nullable Integer>>();",
            "      @Nullable String f2 = \"FOO\";",
            "   }",
            "}")
        .addChanges(
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f0"),
                "edu.ucr.custom.Nullable"),
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f1"),
                "edu.ucr.custom.Nullable",
                ImmutableList.of(
                    ImmutableList.of(0), ImmutableList.of(1, 0), ImmutableList.of(3, 2, 0))),
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f2"),
                "edu.ucr.custom.Nullable"))
        .start();
  }

  @Test
  public void removalOnDeclarationOnly() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import edu.ucr.custom.Nullable;",
            "public class Foo {",
            "   public void foo() {",
            "      @Nullable int f0 = 0;",
            "      @Nullable Bar<@Nullable String, @Nullable Integer, @Nullable Baz<@Nullable String, @Nullable Integer>> f1 = new Custom<@Nullable String, @Nullable String>();",
            "      @Nullable String f2 = \"FOO\";",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.custom.Nullable;",
            "public class Foo {",
            "   public void foo() {",
            "      int f0 = 0;",
            "      Bar<@Nullable String, @Nullable Integer, @Nullable Baz<@Nullable String, @Nullable Integer>> f1 = new Custom<@Nullable String, @Nullable String>();",
            "      String f2 = \"FOO\";",
            "   }",
            "}")
        .addChanges(
            new AddTypeUseMarkerAnnotation(
                    new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f0"),
                    "edu.ucr.custom.Nullable")
                .toDeclaration()
                .getReverse(),
            new AddTypeUseMarkerAnnotation(
                    new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f1"),
                    "edu.ucr.custom.Nullable")
                .toDeclaration()
                .getReverse(),
            new AddTypeUseMarkerAnnotation(
                    new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f2"),
                    "edu.ucr.custom.Nullable")
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
            "import edu.ucr.custom.Nullable;",
            "public class Foo {",
            "   public void foo() {",
            "      @Nullable int f0 = 0;",
            "      @Nullable Bar<@Nullable String, @Nullable Integer, @Nullable Baz<@Nullable String, @Nullable Integer>> f1 = new Bar<@Nullable String, @Nullable Integer, @Nullable Baz<@Nullable String, @Nullable Integer>>();",
            "      @Nullable String f2 = \"FOO\";",
            "      @Nullable Bar<@Nullable String, Integer@Nullable [], @Nullable Baz<@Nullable String, @Nullable Integer>> f3 = new Bar<@Nullable String, Integer@Nullable [], @Nullable Baz<@Nullable String, @Nullable Integer>>();",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.custom.Nullable;",
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
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f0"),
                "edu.ucr.custom.Nullable"),
            new RemoveTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f1"),
                "edu.ucr.custom.Nullable",
                ImmutableList.of(
                    ImmutableList.of(0),
                    ImmutableList.of(1, 0),
                    ImmutableList.of(2, 0),
                    ImmutableList.of(3, 0),
                    ImmutableList.of(3, 1, 0),
                    ImmutableList.of(3, 2, 0))),
            new RemoveTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f2"),
                "edu.ucr.custom.Nullable"),
            new RemoveTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "f3"),
                "edu.ucr.custom.Nullable",
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
            "import edu.ucr.custom.Nullable;",
            "public class Foo {",
            "   public void foo() {",
            "     for (Map.@Nullable Entry<@Nullable String, @Nullable String> entry : m_additionalPreferences.entrySet()) {",
            "       String key = entry.getKey();",
            "       @RUntainted String value = entry.getValue();",
            "       m_user.setAdditionalInfo(PREFERENCES_ADDITIONAL_PREFIX + key, value);",
            "     }",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.custom.Nullable;",
            "public class Foo {",
            "   public void foo() {",
            "     for (Map.@Nullable Entry<@Nullable String, @Nullable String> entry : m_additionalPreferences.entrySet()) {",
            "       String key = entry.getKey();",
            "       @RUntainted String value = entry.getValue();",
            "       m_user.setAdditionalInfo(PREFERENCES_ADDITIONAL_PREFIX + key, value);",
            "     }",
            "   }",
            "}")
        .addChanges(
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "foo()", "entry"),
                "edu.ucr.custom.Nullable"))
        .start();
  }

  @Test
  public void addTypeUseOnType() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import edu.ucr.custom.Nullable;",
            "public class Foo {",
            "   private final Object f0;",
            "   private final Object f1;",
            "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.custom.Nullable;",
            "public class Foo {",
            "   @Nullable private final Object f0;",
            "   private final @Nullable Object f1;",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Collections.singleton("f0")),
                "edu.ucr.custom.Nullable"),
            new AddTypeUseMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Collections.singleton("f1")),
                "edu.ucr.custom.Nullable"))
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
            "import edu.ucr.custom.Nullable;",
            "public class Foo {",
            "   void baz(java.lang.Object param) {",
            "       final java.util.HashMap<java.lang.@Nullable String,java.lang.@Nullable String> localVar = new HashMap<@Nullable String, @Nullable String>();",
            "   }",
            "}")
        .addChanges(
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "baz(java.lang.Object)", "localVar"),
                "edu.ucr.custom.Nullable",
                ImmutableList.of(ImmutableList.of(1, 0))),
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "baz(java.lang.Object)", "localVar"),
                "edu.ucr.custom.Nullable",
                ImmutableList.of(ImmutableList.of(2, 0))))
        .start();
  }

  @Test
  public void deletionOnFullyQualifiedTypeNamesOnTypeArgsTest() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import edu.ucr.custom.Nullable;",
            "public class Foo {",
            "   void baz(java.lang.Object param) {",
            "       final java.util.HashMap<java.lang.@Nullable String,java.lang.@Nullable String> localVar = new HashMap<@Nullable String, @Nullable String>();",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.custom.Nullable;",
            "public class Foo {",
            "   void baz(java.lang.Object param) {",
            "       final java.util.HashMap<java.lang.String,java.lang.String> localVar = new HashMap<String, String>();",
            "   }",
            "}")
        .addChanges(
            new RemoveTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "baz(java.lang.Object)", "localVar"),
                "edu.ucr.custom.Nullable",
                ImmutableList.of(ImmutableList.of(1, 0))),
            new RemoveTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "baz(java.lang.Object)", "localVar"),
                "edu.ucr.custom.Nullable",
                ImmutableList.of(ImmutableList.of(2, 0))))
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
            "import edu.ucr.custom.Nullable;",
            "public class Foo {",
            "   Map<String, @Nullable String> map = new HashMap<String, @Nullable String>();",
            "}")
        .addChanges(
            new AddTypeUseMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Collections.singleton("map")),
                "edu.ucr.custom.Nullable",
                ImmutableList.of(ImmutableList.of(2, 0))))
        .start();
  }

  @Test
  public void deletionOnInitializerFieldTest() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import edu.ucr.custom.Nullable;",
            "public class Foo {",
            "   Map<String, @Nullable String> map = new HashMap<String, @Nullable String>();",
            "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.custom.Nullable;",
            "public class Foo {",
            "   Map<String, String> map = new HashMap<String, String>();",
            "}")
        .addChanges(
            new RemoveTypeUseMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Collections.singleton("map")),
                "edu.ucr.custom.Nullable",
                ImmutableList.of(ImmutableList.of(2, 0))))
        .start();
  }

  @Test
  public void additionOnWildCard() {
    injectorTestHelper
        .addInput("Foo.java", "package test;", "public class Foo {", "   Map<String, ?> map;", "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.custom.Nullable;",
            "public class Foo {",
            "   Map<String, @Nullable ?> map;",
            "}")
        .addChanges(
            new AddTypeUseMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Collections.singleton("map")),
                "edu.ucr.custom.Nullable",
                ImmutableList.of(ImmutableList.of(2, 0))))
        .start();
  }

  @Test
  public void deletionOnWildCard() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import edu.ucr.custom.Nullable;",
            "public class Foo {",
            "   Map<String, @Nullable ?> map;",
            "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.custom.Nullable;",
            "public class Foo {",
            "   Map<String, ?> map;",
            "}")
        .addChanges(
            new RemoveTypeUseMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Collections.singleton("map")),
                "edu.ucr.custom.Nullable",
                ImmutableList.of(ImmutableList.of(2, 0))))
        .start();
  }

  @Test
  public void wildCardExtendedType() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import edu.ucr.custom.Nullable;",
            "public class Foo {",
            "   public synchronized ConfigurationBuilder<? extends HierarchicalConfiguration<?>> m() throws ConfigurationException {}",
            "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.custom.Nullable;",
            "public class Foo {",
            "   public synchronized ConfigurationBuilder<? extends @Nullable HierarchicalConfiguration<?>> m() throws ConfigurationException {}",
            "}")
        .addChanges(
            new AddTypeUseMarkerAnnotation(
                new OnMethod("Foo.java", "test.Foo", "m()"),
                "edu.ucr.custom.Nullable",
                ImmutableList.of(ImmutableList.of(1, 0))))
        .start();
  }

  @Test
  public void existingWildCardExtendedType() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import edu.ucr.custom.Nullable;",
            "public class Foo {",
            "   public synchronized ConfigurationBuilder<? extends @Nullable HierarchicalConfiguration<?>> m() throws ConfigurationException {}",
            "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.custom.Nullable;",
            "public class Foo {",
            "   public synchronized ConfigurationBuilder<? extends @Nullable HierarchicalConfiguration<?>> m() throws ConfigurationException {}",
            "}")
        .addChanges(
            new AddTypeUseMarkerAnnotation(
                new OnMethod("Foo.java", "test.Foo", "m()"),
                "edu.ucr.custom.Nullable",
                ImmutableList.of(ImmutableList.of(1, 0))))
        .start();
  }

  @Test
  public void removeWildCardExtendedType() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import edu.ucr.custom.Nullable;",
            "public class Foo {",
            "   public synchronized ConfigurationBuilder<? extends @Nullable HierarchicalConfiguration<?>> m() throws ConfigurationException {}",
            "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.custom.Nullable;",
            "public class Foo {",
            "   public synchronized ConfigurationBuilder<? extends HierarchicalConfiguration<?>> m() throws ConfigurationException {}",
            "}")
        .addChanges(
            new RemoveTypeUseMarkerAnnotation(
                new OnMethod("Foo.java", "test.Foo", "m()"),
                "edu.ucr.custom.Nullable",
                ImmutableList.of(ImmutableList.of(1, 0))))
        .start();
  }

  @Test
  public void multipleInlineLocalVariableTest() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import edu.ucr.custom.Nullable;",
            "public class Foo {",
            "   public void bar() {",
            "       List<Node> a1 = null, a2 = null;",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import edu.ucr.custom.Nullable;",
            "public class Foo {",
            "   public void bar() {",
            "       @Nullable List<@Nullable Node> a1 = null, a2 = null;",
            "   }",
            "}")
        .addChanges(
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "bar()", "a1"),
                "edu.ucr.custom.Nullable",
                ImmutableList.of(ImmutableList.of(0))),
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "bar()", "a1"),
                "edu.ucr.custom.Nullable",
                ImmutableList.of(ImmutableList.of(1, 0))),
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "bar()", "a2"),
                "edu.ucr.custom.Nullable",
                ImmutableList.of(ImmutableList.of(1, 0))))
        .start();
  }

  @Test
  public void nullableArrayAdditionOnReference() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   Object[] h = new Object[4];",
            "}")
        .expectOutput(
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   Object@Nullable [] h = new Object[4];",
            "}")
        .addChanges(
            new AddTypeUseMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Collections.singleton("h")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void nullableArrayDeletionOnReference() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   Object@Nullable [] h = new Object[4];",
            "}")
        .expectOutput(
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   Object[] h = new Object[4];",
            "}")
        .addChanges(
            new RemoveTypeUseMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Collections.singleton("h")),
                "javax.annotation.Nullable"))
        .start();
  }
}
