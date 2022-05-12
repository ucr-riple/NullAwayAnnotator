package edu.ucr.cs.riple.injector;

import edu.ucr.cs.riple.injector.location.Field;
import edu.ucr.cs.riple.injector.location.Method;
import edu.ucr.cs.riple.injector.location.Parameter;
import edu.ucr.cs.riple.injector.tools.InjectorTestHelper;
import java.util.Collections;
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
                new Method(
                    "Super.java",
                    "com.uber.Super",
                    "test(@javax.annotation.Nullable java.lang.Object)"),
                "javax.annotation.Nullable",
                false))
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
                new Parameter(
                    "Super.java",
                    "com.uber.Super",
                    "test(@javax.annotation.Nullable java.lang.Object)",
                    "o",
                    0),
                "javax.annotation.Nullable",
                false))
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
                new Parameter(
                    "Super.java",
                    "com.uber.Super",
                    "test(@javax.annotation.Nullable java.lang.Object)",
                    "o",
                    0),
                "javax.annotation.Nullable",
                false))
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
                new Field("Super.java", "com.uber.Super", Collections.singleton("f")),
                "javax.annotation.Nullable",
                false))
        .start();
  }
}
