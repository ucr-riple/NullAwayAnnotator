package edu.ucr.cs.riple.injector;

import edu.ucr.cs.riple.injector.location.Field;
import edu.ucr.cs.riple.injector.tools.InjectorTestHelper;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FieldInjectionTest {
  @Test
  public void field_nullable_simple() {
    String rootName = "field_nullable_simple";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   Object h = new Object();",
            "   public void test(@Nullable Object f) {",
            "      h = f;",
            "   }",
            "}")
        .expectOutput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable Object h = new Object();",
            "   public void test(@Nullable Object f) {",
            "      h = f;",
            "   }",
            "}")
        .addChanges(
            new Change(
                new Field("Super.java", "com.uber.Super", Collections.singleton("h")),
                "javax.annotation.Nullable",
                true))
        .start();
  }
}
