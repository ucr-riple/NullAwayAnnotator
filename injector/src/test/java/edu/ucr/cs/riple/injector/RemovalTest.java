package edu.ucr.cs.riple.injector;

import edu.ucr.cs.riple.injector.tools.InjectorTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RemovalTest {

  @Test
  public void remove_annot_return_nullable() {
    String rootName = "remove_annot_return_nullable";
    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable Object test(@javax.annotation.Nullable Object o) {",
            "   }",
            "}")
        .expectOutput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   Object test(@javax.annotation.Nullable Object o) {",
            "   }",
            "}")
        .addChanges(
            new Change(
                "javax.annotation.Nullable",
                "test(@javax.annotation.Nullable java.lang.Object)",
                "",
                "METHOD",
                "com.uber.Super",
                "Super.java",
                "false"))
        .start();
  }

  @Test
  public void remove_annot_param() {
    String rootName = "remove_annot_param";
    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable Object test(@Nullable Object o) {",
            "   }",
            "}")
        .expectOutput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable Object test(Object o) {",
            "   }",
            "}")
        .addChanges(
            new Change(
                "javax.annotation.Nullable",
                "test(@javax.annotation.Nullable java.lang.Object)",
                "o",
                "PARAMETER",
                "com.uber.Super",
                "Super.java",
                "false"))
        .start();
  }

  @Test
  public void remove_annot_param_full_name() {
    String rootName = "remove_annot_param_full_name";
    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable Object test(@javax.annotation.Nullable Object o) {",
            "   }",
            "}")
        .expectOutput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable Object test(Object o) {",
            "   }",
            "}")
        .addChanges(
            new Change(
                "javax.annotation.Nullable",
                "test(@javax.annotation.Nullable java.lang.Object)",
                "o",
                "PARAMETER",
                "com.uber.Super",
                "Super.java",
                "false"))
        .start();
  }

  @Test
  public void remove_annot_field() {
    String rootName = "remove_annot_field";
    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable Object f;",
            "   @Nullable Object test(@javax.annotation.Nullable Object o) {",
            "   }",
            "}")
        .expectOutput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   Object f;",
            "   @Nullable Object test(@javax.annotation.Nullable Object o) {",
            "   }",
            "}")
        .addChanges(
            new Change(
                "javax.annotation.Nullable",
                "",
                "f",
                "FIELD",
                "com.uber.Super",
                "Super.java",
                "false"))
        .start();
  }
}
