package edu.ucr.cs.riple.injector;

import edu.ucr.cs.riple.injector.tools.InjectorTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MethodSearchTest {
  @Test
  public void initializer_constructor() {
    String rootName = "initializer_constructor";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Main.java",
            "package com.uber;",
            "public class Main {",
            "   public Main(String type,Object... objs) {",
            "   }",
            "}")
        .expectOutput(
            "Main.java",
            "package com.uber;",
            "import javax.annotation.Initializer;",
            "public class Main {",
            "   @Initializer",
            "   public Main(String type, Object... objs) {",
            "   }",
            "}")
        .addChanges(
            new Change(
                "javax.annotation.Initializer",
                "Main(java.lang.String,java.lang.Object...)",
                "",
                "METHOD",
                "com.uber.Main",
                "Main.java",
                "true"))
        .start();
  }

  @Test
  public void empty_PARAMETER_pick() {
    String rootName = "empty_PARAMETER_pick";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   Object test() {",
            "       return new Object();",
            "   }",
            "   class SuperInner {",
            "       Object bar(@Nullable Object foo) {",
            "           return foo;",
            "       }",
            "   }",
            "}")
        .expectOutput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable",
            "   Object test() {",
            "       return new Object();",
            "   }",
            "   class SuperInner {",
            "       Object bar(@Nullable Object foo) {",
            "           return foo;",
            "       }",
            "   }",
            "}")
        .addChanges(
            new Change(
                "javax.annotation.Nullable",
                "test()",
                "",
                "METHOD",
                "com.uber.Super",
                "Super.java",
                "true"))
        .start();
  }
}
