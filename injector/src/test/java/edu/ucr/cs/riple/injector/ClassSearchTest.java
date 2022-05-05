package edu.ucr.cs.riple.injector;

import edu.ucr.cs.riple.injector.tools.InjectorTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ClassSearchTest {

  InjectorTestHelper injectorTestHelper;

  @Before
  public void setup() {}

  @Test
  public void class_search_declaration_in_method_body_level_1() {
    String rootName = "class_search_declaration_in_method_body_level_1";

    injectorTestHelper =
        new InjectorTestHelper()
            .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
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
                "Main.java",
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
            .addFixes(
                new Fix(
                    "javax.annotation.Nullable",
                    "get()",
                    "",
                    "METHOD",
                    "com.test.Main$1Bar",
                    "Main.java",
                    "true"));
    injectorTestHelper.start();
  }

  @Test
  public void class_search_declaration_in_method_body_level_2() {
    String rootName = "class_search_declaration_in_method_body_level_2";

    injectorTestHelper =
        new InjectorTestHelper()
            .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
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
                "Main.java",
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
            .addFixes(
                new Fix(
                    "javax.annotation.Nullable",
                    "get()",
                    "",
                    "METHOD",
                    "com.test.Main$1Bar$1Helper",
                    "Main.java",
                    "true"));
    injectorTestHelper.start();
  }

  @Test
  public void class_search_simple_1() {
    String rootName = "class_search_simple_1";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "simple.java")
        .expectOutputFile("Main.java", "simple_expected_1.java")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "null",
                "f1",
                "FIELD",
                "injector.Main$1Helper",
                "Main.java",
                "true"))
        .start(true);
  }

  @Test
  public void class_search_simple_2() {
    String rootName = "class_search_simple_2";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "simple.java")
        .expectOutputFile("Main.java", "simple_expected_2.java")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "null",
                "f0",
                "FIELD",
                "injector.Main$Helper",
                "Main.java",
                "true"))
        .start(true);
  }

  @Test
  public void class_search_simple_3() {
    String rootName = "class_search_simple_3";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "simple.java")
        .expectOutputFile("Main.java", "simple_expected_3.java")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "null",
                "f2",
                "FIELD",
                "injector.Main$2Helper",
                "Main.java",
                "true"))
        .start(true);
  }

  @Test
  public void class_search_benchmark_1() {
    String rootName = "class_search_benchmark_1";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_10.java")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "foo()",
                "",
                "METHOD",
                "injector.Main$1Helper$InnerHelper",
                "Main.java",
                "true"))
        .start(true);
  }

  @Test
  public void class_search_benchmark_10() {
    String rootName = "class_search_benchmark_10";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_10.java")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "foo()",
                "",
                "METHOD",
                "injector.Main$1Helper$InnerHelper",
                "Main.java",
                "true"))
        .start(true);
  }

  @Test
  public void class_search_benchmark_12() {
    String rootName = "class_search_benchmark_12";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_20.java")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "compare()",
                "",
                "METHOD",
                "injector.Main$1$2$1Helper$1",
                "Main.java",
                "true"))
        .start(true);
  }
}
