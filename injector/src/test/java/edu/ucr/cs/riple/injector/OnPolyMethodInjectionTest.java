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

import edu.ucr.cs.riple.injector.changes.AddTypeUseMarkerAnnotation;
import edu.ucr.cs.riple.injector.changes.RemoveTypeUseMarkerAnnotation;
import edu.ucr.cs.riple.injector.location.OnPolyMethod;
import java.util.List;
import org.junit.Test;

public class OnPolyMethodInjectionTest extends BaseInjectorTest {

  @Test
  public void additionTest() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "public class Foo {",
            "   public Object test(Object f, Object f2) {",
            "      return f;",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import custom.annot.Poly;",
            "public class Foo {",
            "   public @Poly Object test(Object f, @Poly Object f2) {",
            "      return f;",
            "   }",
            "}")
        .addChanges(
            new AddTypeUseMarkerAnnotation(
                new OnPolyMethod(
                    "Foo.java", "test.Foo", "test(java.lang.Object,java.lang.Object)", List.of(1)),
                "custom.annot.Poly"))
        .start();
  }

  @Test
  public void deletionTest() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import custom.annot.Poly;",
            "public class Foo {",
            "   public @Poly Object test(Object f, @Poly Object f2) {",
            "      return f;",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import custom.annot.Poly;",
            "public class Foo {",
            "   public Object test(Object f, Object f2) {",
            "      return f;",
            "   }",
            "}")
        .addChanges(
            new RemoveTypeUseMarkerAnnotation(
                new OnPolyMethod(
                    "Foo.java", "test.Foo", "test(java.lang.Object,java.lang.Object)", List.of(1)),
                "custom.annot.Poly"))
        .start();
  }
}