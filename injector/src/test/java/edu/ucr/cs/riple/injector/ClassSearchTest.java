package edu.ucr.cs.riple.injector;

import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.injector.location.OnMethod;
import edu.ucr.cs.riple.injector.tools.InjectorTestHelper;
import java.util.Collections;
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
            .addChanges(
                new Change(
                    new OnMethod("Main.java", "com.test.Main$1Bar", "get()"),
                    "javax.annotation.Nullable",
                    true));
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
            .addChanges(
                new Change(
                    new OnMethod("Main.java", "com.test.Main$1Bar$1Helper", "get()"),
                    "javax.annotation.Nullable",
                    true));
    injectorTestHelper.start();
  }

  @Test
  public void class_search_simple_1() {
    String rootName = "class_search_simple_1";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "simple.java")
        .expectOutputFile("Main.java", "simple_expected_1.java")
        .addChanges(
            new Change(
                new OnField("Main.java", "injector.Main$1Helper", Collections.singleton("f1")),
                "javax.annotation.Nullable",
                true))
        .start(true);
  }

  @Test
  public void class_search_simple_2() {
    String rootName = "class_search_simple_2";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "simple.java")
        .expectOutputFile("Main.java", "simple_expected_2.java")
        .addChanges(
            new Change(
                new OnField("Main.java", "injector.Main$Helper", Collections.singleton("f0")),
                "javax.annotation.Nullable",
                true))
        .start(true);
  }

  @Test
  public void class_search_simple_3() {
    String rootName = "class_search_simple_3";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "simple.java")
        .expectOutputFile("Main.java", "simple_expected_3.java")
        .addChanges(
            new Change(
                new OnField("Main.java", "injector.Main$2Helper", Collections.singleton("f2")),
                "javax.annotation.Nullable",
                true))
        .start(true);
  }

  @Test
  public void class_search_benchmark_1() {
    String rootName = "class_search_benchmark_1";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_1.java")
        .addChanges(
            new Change(
                new OnMethod("Main.java", "injector.Main$Type", "get()"),
                "javax.annotation.Nullable",
                true))
        .start(true);
  }

  @Test
  public void class_search_benchmark_2() {
    String rootName = "class_search_benchmark_2";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_2.java")
        .addChanges(
            new Change(
                new OnMethod("Main.java", "injector.Main$Inner", "bar()"),
                "javax.annotation.Nullable",
                true))
        .start(true);
  }

  @Test
  public void class_search_benchmark_3() {
    String rootName = "class_search_benchmark_3";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_3.java")
        .addChanges(
            new Change(
                new OnField("Main.java", "injector.Main$1", Collections.singleton("f1")),
                "javax.annotation.Nullable",
                true))
        .start(true);
  }

  @Test
  public void class_search_benchmark_4() {
    String rootName = "class_search_benchmark_4";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_4.java")
        .addChanges(
            new Change(
                new OnMethod("Main.java", "injector.Main$1$1", "compare()"),
                "javax.annotation.Nullable",
                true))
        .start(true);
  }

  @Test
  public void class_search_benchmark_5() {
    String rootName = "class_search_benchmark_5";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_5.java")
        .addChanges(
            new Change(
                new OnField("Main.java", "injector.Main$1$2", Collections.singleton("f3")),
                "javax.annotation.Nullable",
                true))
        .start(true);
  }

  @Test
  public void class_search_benchmark_6() {
    String rootName = "class_search_benchmark_6";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_6.java")
        .addChanges(
            new Change(
                new OnField("Main.java", "injector.Main$1$2$1", Collections.singleton("f4")),
                "javax.annotation.Nullable",
                true))
        .start(true);
  }

  @Test
  public void class_search_benchmark_7() {
    String rootName = "class_search_benchmark_7";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_7.java")
        .addChanges(
            new Change(
                new OnField(
                    "Main.java", "injector.Main$1$2$1$1Helper", Collections.singleton("f5")),
                "javax.annotation.Nullable",
                true))
        .start(true);
  }

  @Test
  public void class_search_benchmark_8() {
    String rootName = "class_search_benchmark_8";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_8.java")
        .addChanges(
            new Change(
                new OnField(
                    "Main.java", "injector.Main$1$2$1$1Helper$1", Collections.singleton("f6")),
                "javax.annotation.Nullable",
                true))
        .start(true);
  }

  @Test
  public void class_search_benchmark_9() {
    String rootName = "class_search_benchmark_9";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_9.java")
        .addChanges(
            new Change(
                new OnField(
                    "Main.java", "injector.Main$1$2$1$2Helper", Collections.singleton("f7")),
                "javax.annotation.Nullable",
                true))
        .start(true);
  }

  @Test
  public void class_search_benchmark_10() {
    String rootName = "class_search_benchmark_10";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_10.java")
        .addChanges(
            new Change(
                new OnField(
                    "Main.java", "injector.Main$1$2$1$2Helper$1", Collections.singleton("f8")),
                "javax.annotation.Nullable",
                true))
        .start(true);
  }

  @Test
  public void class_search_benchmark_11() {
    String rootName = "class_search_benchmark_11";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_11.java")
        .addChanges(
            new Change(
                new OnField("Main.java", "injector.Main$1$2$1$Helper", Collections.singleton("f9")),
                "javax.annotation.Nullable",
                true))
        .start(true);
  }

  @Test
  public void class_search_benchmark_12() {
    String rootName = "class_search_benchmark_12";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_12.java")
        .addChanges(
            new Change(
                new OnField(
                    "Main.java", "injector.Main$1$2$1$Helper$1", Collections.singleton("f10")),
                "javax.annotation.Nullable",
                true))
        .start(true);
  }

  @Test
  public void class_search_benchmark_13() {
    String rootName = "class_search_benchmark_13";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_13.java")
        .addChanges(
            new Change(
                new OnField("Main.java", "injector.Main$1$2$1Helper", Collections.singleton("f11")),
                "javax.annotation.Nullable",
                true))
        .start(true);
  }

  @Test
  public void class_search_benchmark_14() {
    String rootName = "class_search_benchmark_14";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_14.java")
        .addChanges(
            new Change(
                new OnField(
                    "Main.java", "injector.Main$1$2$1Helper$1", Collections.singleton("f12")),
                "javax.annotation.Nullable",
                true))
        .start(true);
  }

  @Test
  public void class_search_benchmark_15() {
    String rootName = "class_search_benchmark_15";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_15.java")
        .addChanges(
            new Change(
                new OnField("Main.java", "injector.Main$2", Collections.singleton("f13")),
                "javax.annotation.Nullable",
                true))
        .start(true);
  }

  @Test
  public void class_search_benchmark_16() {
    String rootName = "class_search_benchmark_16";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_16.java")
        .addChanges(
            new Change(
                new OnField("Main.java", "injector.Main$1Helper", Collections.singleton("f14")),
                "javax.annotation.Nullable",
                true))
        .start(true);
  }

  @Test
  public void class_search_benchmark_17() {
    String rootName = "class_search_benchmark_17";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_17.java")
        .addChanges(
            new Change(
                new OnMethod("Main.java", "injector.Main$1Helper$InnerHelper", "foo()"),
                "javax.annotation.Nullable",
                true))
        .start(true);
  }

  @Test
  public void class_search_benchmark_18() {
    String rootName = "class_search_benchmark_18";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_18.java")
        .addChanges(
            new Change(
                new OnField("Main.java", "injector.Main$1Helper$1", Collections.singleton("f16")),
                "javax.annotation.Nullable",
                true))
        .start(true);
  }

  @Test
  public void class_search_benchmark_19() {
    String rootName = "class_search_benchmark_19";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_19.java")
        .addChanges(
            new Change(
                new OnField("Main.java", "injector.Main$3", Collections.singleton("f17")),
                "javax.annotation.Nullable",
                true))
        .start(true);
  }

  @Test
  public void class_search_benchmark_20() {
    String rootName = "class_search_benchmark_20";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_20.java")
        .addChanges(
            new Change(
                new OnField("Main.java", "injector.Main$2Helper", Collections.singleton("f18")),
                "javax.annotation.Nullable",
                true))
        .start(true);
  }

  @Test
  public void class_search_benchmark_21() {
    String rootName = "class_search_benchmark_21";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_21.java")
        .addChanges(
            new Change(
                new OnField("Main.java", "injector.Main$3Helper", Collections.singleton("f19")),
                "javax.annotation.Nullable",
                true))
        .start(true);
  }

  @Test
  public void class_search_benchmark_22() {
    String rootName = "class_search_benchmark_22";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "benchmark.java")
        .expectOutputFile("Main.java", "benchmark_expected_22.java")
        .addChanges(
            new Change(
                new OnMethod("Main.java", "injector.Outer", "foo()"),
                "javax.annotation.Nullable",
                true))
        .start(true);
  }
}
