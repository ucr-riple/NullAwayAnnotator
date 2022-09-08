/*
 * MIT License
 *
 * Copyright (c) 2022 Nima Karimipour
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

import edu.ucr.cs.riple.injector.changes.AddAnnotation;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.injector.location.OnMethod;
import edu.ucr.cs.riple.injector.location.OnParameter;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AnnotationWithArgumentTest extends BaseInjectorTest {
  @Test
  public void onFieldSuppressWarningTest() {
    injectorTestHelper
        .addInput(
            "Super.java",
            "package com.uber;",
            "public class Super {",
            "   Object h = new Object();",
            "   public void test(Object f) {",
            "      h = f;",
            "   }",
            "}")
        .expectOutput(
            "package com.uber;",
            "public class Super {",
            "   @SuppressWarning(\"NullAway.init\")",
            "   Object h = new Object();",
            "   public void test(Object f) {",
            "      h = f;",
            "   }",
            "}")
        .addChanges(
            new AddAnnotation(
                new OnField("Super.java", "com.uber.Super", Collections.singleton("h")),
                "SuppressWarning",
                "NullAway.init"))
        .start();
  }

  @Test
  public void onFieldSuppressWarningAlreadyExistsTest() {
    injectorTestHelper
        .addInput(
            "Super.java",
            "package com.uber;",
            "public class Super {",
            "   @SuppressWarning(\"NullAway.init\")",
            "   Object h = new Object();",
            "   public void test(Object f) {",
            "      h = f;",
            "   }",
            "}")
        .expectOutput(
            "package com.uber;",
            "public class Super {",
            "   @SuppressWarning(\"NullAway.init\")",
            "   Object h = new Object();",
            "   public void test(Object f) {",
            "      h = f;",
            "   }",
            "}")
        .addChanges(
            new AddAnnotation(
                new OnField("Super.java", "com.uber.Super", Collections.singleton("h")),
                "SuppressWarning",
                "NullAway.init"))
        .start();
  }

  @Test
  public void onFieldSuppressWarningOtherExistsTest() {
    injectorTestHelper
        .addInput(
            "Super.java",
            "package com.uber;",
            "public class Super {",
            "   @SuppressWarning(\"something else\")",
            "   Object h = new Object();",
            "   public void test(Object f) {",
            "      h = f;",
            "   }",
            "}")
        .expectOutput(
            "package com.uber;",
            "public class Super {",
            "   @SuppressWarning(\"something else\")",
            "   @SuppressWarning(\"NullAway.init\")",
            "   Object h = new Object();",
            "   public void test(Object f) {",
            "      h = f;",
            "   }",
            "}")
        .addChanges(
            new AddAnnotation(
                new OnField("Super.java", "com.uber.Super", Collections.singleton("h")),
                "SuppressWarning",
                "NullAway.init"))
        .start();
  }

  @Test
  public void onMethodNullUnmarkedTest() {
    injectorTestHelper
        .addInput(
            "Super.java",
            "package com.uber;",
            "public class Super {",
            "   Object h = new Object();",
            "   public void test(Object f) {",
            "      h = f;",
            "   }",
            "}")
        .expectOutput(
            "package com.uber;",
            "import edu.ucr.NullUnmarked;",
            "public class Super {",
            "   Object h = new Object();",
            "   @NullUnmarked(\"Prefix: Creates 2 errors on downstream dependencies\")",
            "   public void test(Object f) {",
            "      h = f;",
            "   }",
            "}")
        .addChanges(
            new AddAnnotation(
                new OnMethod("Super.java", "com.uber.Super", "test(java.lang.Object)"),
                "edu.ucr.NullUnmarked",
                "Prefix: Creates 2 errors on downstream dependencies"))
        .start();
  }

  @Test
  public void onParameterCustomAnnotationTest() {
    injectorTestHelper
        .addInput(
            "Super.java",
            "package com.uber;",
            "public class Super {",
            "   Object h = new Object();",
            "   public void test(Object f) {",
            "      h = f;",
            "   }",
            "}")
        .expectOutput(
            "package com.uber;",
            "import edu.ucr.NullMessage;",
            "public class Super {",
            "   Object h = new Object();",
            "   public void test(@NullMessage(\"Receives null from downstream\") Object f) {",
            "      h = f;",
            "   }",
            "}")
        .addChanges(
            new AddAnnotation(
                new OnParameter("Super.java", "com.uber.Super", "test(java.lang.Object)", 0),
                "edu.ucr.NullMessage",
                "Receives null from downstream"))
        .start();
  }
}
