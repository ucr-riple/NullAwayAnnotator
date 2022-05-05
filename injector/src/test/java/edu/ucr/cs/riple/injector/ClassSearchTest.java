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
        .expectOutputFile("Main.java", "benchmark_expected_1.java")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "get()",
                "",
                "METHOD",
                "injector.Main$Type",
                "Main.java",
                "true"))
        .start(true);
  }

  @Test
  public void class_search_benchmark_2() {
    String rootName = "class_search_benchmark_2";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_2.java")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "bar()",
                "",
                "METHOD",
                "injector.Main$Inner",
                "Main.java",
                "true"))
        .start(true);
  }

  @Test
  public void class_search_benchmark_3() {
    String rootName = "class_search_benchmark_3";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_3.java")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "null",
                "f1",
                "FIELD",
                "injector.Main$1",
                "Main.java",
                "true"))
        .start(true);
  }

  @Test
  public void class_search_benchmark_4() {
    String rootName = "class_search_benchmark_4";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_4.java")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "compare()",
                "null",
                "METHOD",
                "injector.Main$1$1",
                "Main.java",
                "true"))
        .start(true);
  }

  @Test
  public void class_search_benchmark_5() {
    String rootName = "class_search_benchmark_5";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_5.java")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "null",
                "f3",
                "FIELD",
                "injector.Main$1$2",
                "Main.java",
                "true"))
        .start(true);
  }

  @Test
  public void class_search_benchmark_6() {
    String rootName = "class_search_benchmark_6";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_6.java")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "null",
                "f4",
                "FIELD",
                "injector.Main$1$2$1",
                "Main.java",
                "true"))
        .start(true);
  }

  @Test
  public void class_search_benchmark_7() {
    String rootName = "class_search_benchmark_7";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_7.java")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "null",
                "f5",
                "FIELD",
                "injector.Main$1$2$1$1Helper",
                "Main.java",
                "true"))
        .start(true);
  }

  @Test
  public void class_search_benchmark_8() {
    String rootName = "class_search_benchmark_8";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_8.java")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "null",
                "f6",
                "FIELD",
                "injector.Main$1$2$1$1Helper$1",
                "Main.java",
                "true"))
        .start(true);
  }

  @Test
  public void class_search_benchmark_9() {
    String rootName = "class_search_benchmark_9";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_9.java")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "null",
                "f7",
                "FIELD",
                "injector.Main$1$2$1$2Helper",
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
                "null",
                "f8",
                "FIELD",
                "injector.Main$1$2$1$2Helper$1",
                "Main.java",
                "true"))
        .start(true);
  }

  @Test
  public void class_search_benchmark_11() {
    String rootName = "class_search_benchmark_11";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_11.java")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "null",
                "f9",
                "FIELD",
                "injector.Main$1$2$1$Helper",
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
        .expectOutputFile("Main.java", "benchmark_expected_12.java")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "null",
                "f10",
                "FIELD",
                "injector.Main$1$2$1$Helper$1",
                "Main.java",
                "true"))
        .start(true);
  }

  @Test
  public void class_search_benchmark_13() {
    String rootName = "class_search_benchmark_13";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_13.java")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "null",
                "f11",
                "FIELD",
                "injector.Main$1$2$1Helper",
                "Main.java",
                "true"))
        .start(true);
  }

  @Test
  public void class_search_benchmark_14() {
    String rootName = "class_search_benchmark_14";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_14.java")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "null",
                "f12",
                "FIELD",
                "injector.Main$1$2$1Helper$1",
                "Main.java",
                "true"))
        .start(true);
  }

  @Test
  public void class_search_benchmark_15() {
    String rootName = "class_search_benchmark_15";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_15.java")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "null",
                "f13",
                "FIELD",
                "injector.Main$2",
                "Main.java",
                "true"))
        .start(true);
  }

  @Test
  public void class_search_benchmark_16() {
    String rootName = "class_search_benchmark_16";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_16.java")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "null",
                "f14",
                "FIELD",
                "injector.Main$1Helper",
                "Main.java",
                "true"))
        .start(true);
  }

  @Test
  public void class_search_benchmark_17() {
    String rootName = "class_search_benchmark_17";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_17.java")
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
  public void class_search_benchmark_18() {
    String rootName = "class_search_benchmark_18";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_18.java")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "null",
                "f16",
                "FIELD",
                "injector.Main$1Helper$1",
                "Main.java",
                "true"))
        .start(true);
  }

  @Test
  public void class_search_benchmark_19() {
    String rootName = "class_search_benchmark_19";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_19.java")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "null",
                "f17",
                "FIELD",
                "injector.Main$3",
                "Main.java",
                "true"))
        .start(true);
  }

  @Test
  public void class_search_benchmark_20() {
    String rootName = "class_search_benchmark_20";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_20.java")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "null",
                "f18",
                "FIELD",
                "injector.Main$2Helper",
                "Main.java",
                "true"))
        .start(true);
  }

  @Test
  public void class_search_benchmark_21() {
    String rootName = "class_search_benchmark_21";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_21.java")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "null",
                "f19",
                "FIELD",
                "injector.Main$3Helper",
                "Main.java",
                "true"))
        .start(true);
  }

  @Test
  public void class_search_benchmark_22() {
    String rootName = "class_search_benchmark_22";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_22.java")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "foo()",
                "null",
                "METHOD",
                "injector.Outer",
                "Main.java",
                "true"))
        .start(true);
  }
}
