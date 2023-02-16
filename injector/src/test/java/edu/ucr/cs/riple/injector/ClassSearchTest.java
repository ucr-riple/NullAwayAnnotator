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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import edu.ucr.cs.riple.injector.changes.AddMarkerAnnotation;
import edu.ucr.cs.riple.injector.exceptions.TargetClassNotFound;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.injector.location.OnMethod;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ClassSearchTest extends BaseInjectorTest {

  @Test
  public void annotation_declaration() {
    injectorTestHelper
        .addInput(
            "Main.java",
            "package com.test;",
            "public @interface Main{",
            "   public String foo();",
            "}")
        .expectOutput(
            "package com.test;",
            "import javax.annotation.Nullable;",
            "public @interface Main{",
            "   @Nullable public String foo();",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnMethod("Main.java", "com.test.Main", "foo(java.lang.Object)"),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void class_search_declaration_in_method_body_level_1() {
    injectorTestHelper
        .addInput(
            "Main.java",
            "package com.test;",
            "public class Main implements Runnable {",
            "   @Override",
            "   public Object foo() {",
            "     class Bar {",
            "        int i = 5 * 6;",
            "        public Object get() { return null; }",
            "     }",
            "   }",
            "}")
        .expectOutput(
            "package com.test;",
            "import javax.annotation.Nullable;",
            "public class Main implements Runnable {",
            "   @Override",
            "   public Object foo() {",
            "     class Bar {",
            "        int i = 5 * 6;",
            "        @Nullable public Object get() { return null; }",
            "     }",
            "   }",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnMethod("Main.java", "com.test.Main$1Bar", "get()"),
                "javax.annotation.Nullable"));
    injectorTestHelper.start();
  }

  @Test
  public void class_search_declaration_in_method_body_level_2() {
    injectorTestHelper
        .addInput(
            "Main.java",
            "package com.test;",
            "public class Main implements Runnable {",
            "   @Override",
            "   public Object foo() {",
            "     class Bar {",
            "        int i = 5 * 6;",
            "        public Object get() {",
            "           class Helper{",
            "              public get() { return null; }",
            "           }",
            "           return null;",
            "        }",
            "     }",
            "   }",
            "}")
        .expectOutput(
            "package com.test;",
            "import javax.annotation.Nullable;",
            "public class Main implements Runnable {",
            "   @Override",
            "   public Object foo() {",
            "     class Bar {",
            "        int i = 5 * 6;",
            "        public Object get() {",
            "           class Helper{",
            "              @Nullable public get() { return null; }",
            "           }",
            "           return null;",
            "        }",
            "     }",
            "   }",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnMethod("Main.java", "com.test.Main$1Bar$1Helper", "get()"),
                "javax.annotation.Nullable"));
    injectorTestHelper.start();
  }

  @Test
  public void class_search_simple_1() {
    injectorTestHelper
        .addInputSourceFile("Main.java", "simple.java")
        .expectOutputFile("simple_expected_1.java")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField("Main.java", "injector.Main$1Helper", Collections.singleton("f1")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void class_search_simple_2() {
    injectorTestHelper
        .addInputSourceFile("Main.java", "simple.java")
        .expectOutputFile("simple_expected_2.java")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField("Main.java", "injector.Main$Helper", Collections.singleton("f0")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void class_search_simple_3() {
    injectorTestHelper
        .addInputSourceFile("Main.java", "simple.java")
        .expectOutputFile("simple_expected_3.java")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField("Main.java", "injector.Main$2Helper", Collections.singleton("f2")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void class_search_benchmark_1() {
    injectorTestHelper
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("benchmark_expected_1.java")
        .addChanges(
            new AddMarkerAnnotation(
                new OnMethod("Main.java", "injector.Main$Type", "get()"),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void class_search_benchmark_2() {
    injectorTestHelper
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("benchmark_expected_2.java")
        .addChanges(
            new AddMarkerAnnotation(
                new OnMethod("Main.java", "injector.Main$Inner", "bar()"),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void class_search_benchmark_3() {
    injectorTestHelper
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("benchmark_expected_3.java")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField("Main.java", "injector.Main$1", Collections.singleton("f1")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void class_search_benchmark_4() {
    injectorTestHelper
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("benchmark_expected_4.java")
        .addChanges(
            new AddMarkerAnnotation(
                new OnMethod("Main.java", "injector.Main$1$1", "compare()"),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void class_search_benchmark_5() {
    injectorTestHelper
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("benchmark_expected_5.java")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField("Main.java", "injector.Main$1$2", Collections.singleton("f3")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void class_search_benchmark_6() {
    injectorTestHelper
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("benchmark_expected_6.java")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField("Main.java", "injector.Main$1$2$1", Collections.singleton("f4")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void class_search_benchmark_7() {
    injectorTestHelper
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("benchmark_expected_7.java")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField(
                    "Main.java", "injector.Main$1$2$1$1Helper", Collections.singleton("f5")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void class_search_benchmark_8() {
    injectorTestHelper
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("benchmark_expected_8.java")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField(
                    "Main.java", "injector.Main$1$2$1$1Helper$1", Collections.singleton("f6")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void class_search_benchmark_9() {
    injectorTestHelper
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("benchmark_expected_9.java")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField(
                    "Main.java", "injector.Main$1$2$1$2Helper", Collections.singleton("f7")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void class_search_benchmark_10() {
    injectorTestHelper
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("benchmark_expected_10.java")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField(
                    "Main.java", "injector.Main$1$2$1$2Helper$1", Collections.singleton("f8")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void class_search_benchmark_11() {
    injectorTestHelper
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("benchmark_expected_11.java")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField("Main.java", "injector.Main$1$2$1$Helper", Collections.singleton("f9")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void class_search_benchmark_12() {
    injectorTestHelper
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("benchmark_expected_12.java")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField(
                    "Main.java", "injector.Main$1$2$1$Helper$1", Collections.singleton("f10")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void class_search_benchmark_13() {
    injectorTestHelper
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("benchmark_expected_13.java")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField("Main.java", "injector.Main$1$2$1Helper", Collections.singleton("f11")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void class_search_benchmark_14() {
    injectorTestHelper
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("benchmark_expected_14.java")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField(
                    "Main.java", "injector.Main$1$2$1Helper$1", Collections.singleton("f12")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void class_search_benchmark_15() {
    injectorTestHelper
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("benchmark_expected_15.java")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField("Main.java", "injector.Main$2", Collections.singleton("f13")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void class_search_benchmark_16() {
    injectorTestHelper
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("benchmark_expected_16.java")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField("Main.java", "injector.Main$1Helper", Collections.singleton("f14")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void class_search_benchmark_17() {
    injectorTestHelper
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("benchmark_expected_17.java")
        .addChanges(
            new AddMarkerAnnotation(
                new OnMethod("Main.java", "injector.Main$1Helper$InnerHelper", "foo()"),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void class_search_benchmark_18() {
    injectorTestHelper
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("benchmark_expected_18.java")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField("Main.java", "injector.Main$1Helper$1", Collections.singleton("f16")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void class_search_benchmark_19() {
    injectorTestHelper
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("benchmark_expected_19.java")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField("Main.java", "injector.Main$3", Collections.singleton("f17")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void class_search_benchmark_20() {
    injectorTestHelper
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("benchmark_expected_20.java")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField("Main.java", "injector.Main$2Helper", Collections.singleton("f18")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void class_search_benchmark_21() {
    injectorTestHelper
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("benchmark_expected_21.java")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField("Main.java", "injector.Main$3Helper", Collections.singleton("f19")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void class_search_benchmark_22() {
    injectorTestHelper
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("benchmark_expected_22.java")
        .addChanges(
            new AddMarkerAnnotation(
                new OnMethod("Main.java", "injector.Outer", "foo()"), "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void proper_report_in_err_std_for_class_not_found_test() {
    String expectedErrorMessage = "Could not find class of type: Top-Level with name: NotIncluded";
    String[] clazzLines =
        new String[] {
          "package com.test;", "public @interface Main{", "   public String foo();", "}"
        };
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    System.setErr(new PrintStream(err));
    injectorTestHelper
        .addInput("Main.java", clazzLines)
        .expectOutput(clazzLines)
        .addChanges(
            new AddMarkerAnnotation(
                new OnMethod("Main.java", "com.test.NotIncluded", "foo(java.lang.Object)"),
                "javax.annotation.Nullable"))
        .start();
    assertTrue(err.toString().contains(expectedErrorMessage));
  }

  @Test
  public void proper_exception_check_for_class_not_found_test() {
    String expectedErrorMessage = "Could not find class of type: Top-Level with name: NotIncluded";
    String[] clazzLines =
        new String[] {
          "package com.test;", "public @interface Main{", "   public String foo();", "}"
        };
    CompilationUnit tree = StaticJavaParser.parse(String.join("\n", clazzLines));
    TargetClassNotFound thrown =
        assertThrows(
            TargetClassNotFound.class,
            () -> Helper.getTypeDeclarationMembersByFlatName(tree, "com.test.NotIncluded"));
    assertTrue(thrown.getMessage().contains(expectedErrorMessage));
  }

  @Test
  public void enumConstantSearch1() {
    injectorTestHelper
        .addInputSourceFile("Main.java", "enum_search.java")
        .expectOutputFile("enum_search_expected_1.java")
        .addChanges(
            new AddMarkerAnnotation(
                new OnMethod("Main.java", "injector.Main$1", "bar()"), "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void enumConstantSearch2() {
    injectorTestHelper
        .addInputSourceFile("Main.java", "enum_search.java")
        .expectOutputFile("enum_search_expected_2.java")
        .addChanges(
            new AddMarkerAnnotation(
                new OnMethod("Main.java", "injector.Main$2", "bar()"), "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void enumConstantSearch3() {
    injectorTestHelper
        .addInputSourceFile("Main.java", "enum_search.java")
        .expectOutputFile("enum_search_expected_3.java")
        .addChanges(
            new AddMarkerAnnotation(
                new OnMethod("Main.java", "injector.Main$3", "bar()"), "javax.annotation.Nullable"))
        .start();
  }
}
