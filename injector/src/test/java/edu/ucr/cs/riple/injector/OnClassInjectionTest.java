/*
 * MIT License
 *
 * Copyright (c) 2023 Nima Karimipour
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

import edu.ucr.cs.riple.injector.changes.AddMarkerAnnotation;
import edu.ucr.cs.riple.injector.location.OnClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class OnClassInjectionTest extends BaseInjectorTest {

  @Test
  public void onClassBasicTest() {
    injectorTestHelper
        .addInput(
            "Super.java",
            "package com.uber;",
            "import edu.custom.NullUnmarked;",
            "public class Super {",
            "   Object h = new Object();",
            "   public void test(Object f) {",
            "      h = f;",
            "   }",
            "}")
        .expectOutput(
            "package com.uber;",
            "import edu.custom.NullUnmarked;",
            "@NullUnmarked public class Super {",
            "   Object h = new Object();",
            "   public void test(Object f) {",
            "      h = f;",
            "   }",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnClass("Super.java", "com.uber.Super"), "edu.custom.NullUnmarked"))
        .start();
  }

  @Test
  public void skipOnAnonymousClassTest() {
    injectorTestHelper
        .addInput(
            "Test.java",
            "package edu.ucr;",
            "import edu.custom.NullUnmarked;",
            "public class Test {",
            "   public void test() {",
            "       I i = new I() {",
            "           @Override",
            "           public void test() { }",
            "       };",
            "   }",
            "}",
            "interface I {",
            "  void test();",
            "}")
        .expectOutput(
            "package edu.ucr;",
            "import edu.custom.NullUnmarked;",
            "public class Test {",
            "   public void test() {",
            "       I i = new I() {",
            "           @Override",
            "           public void test() { }",
            "       };",
            "   }",
            "}",
            "interface I {",
            "  void test();",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnClass("Test.java", "edu.ucr.Test$1"), "edu.custom.NullUnmarked"))
        .start();
  }
}
