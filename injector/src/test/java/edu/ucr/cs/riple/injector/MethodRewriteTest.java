/*
 * MIT License
 *
 * Copyright (c) 2025 Nima Karimipour
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

import edu.ucr.cs.riple.injector.changes.MethodRewriteChange;
import edu.ucr.cs.riple.injector.location.OnMethod;
import org.junit.Test;

public class MethodRewriteTest extends BaseInjectorTest {

  @Test
  public void singleMethodRewriteBasicTest() {
    final String rewriteContent =
        "public String bar(Object param) {\n"
            + "   return f == null ? \"default\" : f.toString();\n"
            + "}";
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   @Nullable Object f;",
            "   @Nullable public String bar(@Nullable Object param) {",
            "      return f.toString();",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   @Nullable Object f;",
            "   @Nullable public String bar(@Nullable Object param) {",
            "      return f == null ? \"default\" : f.toString();",
            "   }",
            "}")
        .addChanges(
            new MethodRewriteChange(
                new OnMethod("Foo.java", "test.Foo", "bar(Object)"), rewriteContent))
        .rewrite();
  }
}
