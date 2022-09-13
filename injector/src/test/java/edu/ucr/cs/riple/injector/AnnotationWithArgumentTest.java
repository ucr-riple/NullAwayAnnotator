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

import edu.ucr.cs.riple.injector.changes.AddSingleElementAnnotation;
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
            "package com.edu;",
            "public class Super {",
            "   Object h = new Object();",
            "   public void test(Object f) {",
            "      h = f;",
            "   }",
            "}")
        .expectOutput(
            "package com.edu;",
            "public class Super {",
            "   @SuppressWarnings(\"NullAway.Init\")",
            "   Object h = new Object();",
            "   public void test(Object f) {",
            "      h = f;",
            "   }",
            "}")
        .addChanges(
            new AddSingleElementAnnotation(
                new OnField("Super.java", "com.edu.Super", Collections.singleton("h")),
                "SuppressWarnings",
                "NullAway.Init",
                false))
        .start();
  }

  @Test
  public void onFieldSuppressWarningAlreadyExistsTest() {
    injectorTestHelper
        .addInput(
            "Super.java",
            "package com.edu;",
            "public class Super {",
            "   @SuppressWarnings(\"NullAway.Init\")",
            "   Object h = new Object();",
            "   public void test(Object f) {",
            "      h = f;",
            "   }",
            "}")
        .expectOutput(
            "package com.edu;",
            "public class Super {",
            "   @SuppressWarnings(\"NullAway.Init\")",
            "   Object h = new Object();",
            "   public void test(Object f) {",
            "      h = f;",
            "   }",
            "}")
        .addChanges(
            new AddSingleElementAnnotation(
                new OnField("Super.java", "com.edu.Super", Collections.singleton("h")),
                "SuppressWarnings",
                "NullAway.Init",
                false))
        .start();
  }

  @Test
  public void onFieldSuppressWarningOtherExistsTest() {
    injectorTestHelper
        .addInput(
            "Super.java",
            "package com.edu;",
            "public class Super {",
            "   @SuppressWarnings(\"something else\")",
            "   Object h = new Object();",
            "   public void test(Object f) {",
            "      h = f;",
            "   }",
            "}")
        .expectOutput(
            "package com.edu;",
            "public class Super {",
            "   @SuppressWarnings({\"something else\", \"NullAway.Init\"})",
            "   Object h = new Object();",
            "   public void test(Object f) {",
            "      h = f;",
            "   }",
            "}")
        .addChanges(
            new AddSingleElementAnnotation(
                new OnField("Super.java", "com.edu.Super", Collections.singleton("h")),
                "SuppressWarnings",
                "NullAway.Init",
                false))
        .start();
  }

  @Test
  public void onMethodCustomAnnotTest() {
    injectorTestHelper
        .addInput(
            "Super.java",
            "package com.edu;",
            "public class Super {",
            "   Object h = new Object();",
            "   public void test(Object f) {",
            "      h = f;",
            "   }",
            "}")
        .expectOutput(
            "package com.edu;",
            "import edu.ucr.CustomNull;",
            "public class Super {",
            "   Object h = new Object();",
            "   @CustomNull(\"Prefix: Creates 2 errors on downstream dependencies\")",
            "   public void test(Object f) {",
            "      h = f;",
            "   }",
            "}")
        .addChanges(
            new AddSingleElementAnnotation(
                new OnMethod("Super.java", "com.edu.Super", "test(java.lang.Object)"),
                "edu.ucr.CustomNull",
                "Prefix: Creates 2 errors on downstream dependencies",
                false))
        .start();
  }

  @Test
  public void onParameterCustomAnnotationTest() {
    injectorTestHelper
        .addInput(
            "Super.java",
            "package com.edu;",
            "public class Super {",
            "   Object h = new Object();",
            "   public void test(Object f) {",
            "      h = f;",
            "   }",
            "}")
        .expectOutput(
            "package com.edu;",
            "import edu.ucr.NullMessage;",
            "public class Super {",
            "   Object h = new Object();",
            "   public void test(@NullMessage(\"Receives null from downstream\") Object f) {",
            "      h = f;",
            "   }",
            "}")
        .addChanges(
            new AddSingleElementAnnotation(
                new OnParameter("Super.java", "com.edu.Super", "test(java.lang.Object)", 0),
                "edu.ucr.NullMessage",
                "Receives null from downstream",
                false))
        .start();
  }

  @Test
  public void onMethodCustomAnnotSingleArgExistsTest() {
    injectorTestHelper
        .addInput(
            "Super.java",
            "package com.edu;",
            "public class Super {",
            "   Object h = new Object();",
            "   @CustomNull(\"arg1\")",
            "   public void test(Object f) {",
            "      h = f;",
            "   }",
            "}")
        .expectOutput(
            "package com.edu;",
            "import edu.ucr.CustomNull;",
            "public class Super {",
            "   Object h = new Object();",
            "   @CustomNull(\"arg1\")",
            "   public void test(Object f) {",
            "      h = f;",
            "   }",
            "}")
        .addChanges(
            new AddSingleElementAnnotation(
                new OnMethod("Super.java", "com.edu.Super", "test(java.lang.Object)"),
                "edu.ucr.CustomNull",
                "arg1",
                false))
        .start();
  }

  @Test
  public void onMethodCustomAnnotMultiArgExistsTest() {
    injectorTestHelper
        .addInput(
            "Super.java",
            "package com.edu;",
            "public class Super {",
            "   Object h = new Object();",
            "   @Other({\"arg1\", \"arg2\"})",
            "   @CustomNull({\"arg1\", \"arg2\"})",
            "   public void test(Object f) {",
            "      h = f;",
            "   }",
            "}")
        .expectOutput(
            "package com.edu;",
            "import edu.ucr.CustomNull;",
            "public class Super {",
            "   Object h = new Object();",
            "   @Other({\"arg1\", \"arg2\"})",
            "   @CustomNull({\"arg1\", \"arg2\"})",
            "   public void test(Object f) {",
            "      h = f;",
            "   }",
            "}")
        .addChanges(
            new AddSingleElementAnnotation(
                new OnMethod("Super.java", "com.edu.Super", "test(java.lang.Object)"),
                "edu.ucr.CustomNull",
                "arg1",
                false))
        .start();
  }

  @Test
  public void onMethodRepeatableOnTest() {
    injectorTestHelper
        .addInput(
            "Super.java",
            "package com.edu;",
            "public class Super {",
            "   Object h = new Object();",
            "   @CustomNull({\"arg1\", \"arg2\"})",
            "   public void test(Object f) {",
            "      h = f;",
            "   }",
            "}")
        .expectOutput(
            "package com.edu;",
            "import edu.ucr.CustomNull;",
            "public class Super {",
            "   Object h = new Object();",
            "   @CustomNull({\"arg1\", \"arg2\", \"arg3\"})",
            "   public void test(Object f) {",
            "      h = f;",
            "   }",
            "}")
        .addChanges(
            new AddSingleElementAnnotation(
                new OnMethod("Super.java", "com.edu.Super", "test(java.lang.Object)"),
                "edu.ucr.CustomNull",
                "arg3",
                false))
        .start();
  }

  @Test
  public void onMethodRepeatableOffTest() {
    injectorTestHelper
        .addInput(
            "Super.java",
            "package com.edu;",
            "public class Super {",
            "   Object h = new Object();",
            "   @CustomNull({\"arg1\", \"arg2\"})",
            "   public void test(Object f) {",
            "      h = f;",
            "   }",
            "}")
        .expectOutput(
            "package com.edu;",
            "import edu.ucr.CustomNull;",
            "public class Super {",
            "   Object h = new Object();",
            "   @CustomNull({\"arg1\", \"arg2\"})",
            "   @CustomNull(\"arg3\")",
            "   public void test(Object f) {",
            "      h = f;",
            "   }",
            "}")
        .addChanges(
            new AddSingleElementAnnotation(
                new OnMethod("Super.java", "com.edu.Super", "test(java.lang.Object)"),
                "edu.ucr.CustomNull",
                "arg3",
                true))
        .start();
  }

  @Test
  public void testImportNotExistsRepeatableOff() {
    injectorTestHelper
        .addInput(
            "Super.java",
            "package com.edu;",
            "public class Super {",
            "   Object h = new Object();",
            "   public void test(Object f) {",
            "      h = f;",
            "   }",
            "}")
        .expectOutput(
            "package com.edu;",
            "import edu.ucr.CustomNull;",
            "public class Super {",
            "   Object h = new Object();",
            "   @CustomNull(\"arg3\")",
            "   public void test(Object f) {",
            "      h = f;",
            "   }",
            "}")
        .addChanges(
            new AddSingleElementAnnotation(
                new OnMethod("Super.java", "com.edu.Super", "test(java.lang.Object)"),
                "edu.ucr.CustomNull",
                "arg3",
                true))
        .start();
  }

  @Test
  public void testImportNotExistsRepeatableOn() {
    injectorTestHelper
        .addInput(
            "Super.java",
            "package com.edu;",
            "public class Super {",
            "   Object h = new Object();",
            "   public void test(Object f) {",
            "      h = f;",
            "   }",
            "}")
        .expectOutput(
            "package com.edu;",
            "import edu.ucr.CustomNull;",
            "public class Super {",
            "   Object h = new Object();",
            "   @CustomNull(\"arg3\")",
            "   public void test(Object f) {",
            "      h = f;",
            "   }",
            "}")
        .addChanges(
            new AddSingleElementAnnotation(
                new OnMethod("Super.java", "com.edu.Super", "test(java.lang.Object)"),
                "edu.ucr.CustomNull",
                "arg3",
                false))
        .start();
  }
}
