/*
 * MIT License
 *
 * Copyright (c) 2020 Nima Karimipour
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
import edu.ucr.cs.riple.injector.location.OnField;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class OnFieldInjectionTest extends BaseInjectorTest {
  @Test
  public void fieldNullableSimple() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   Object h = new Object();",
            "   public void test(@Nullable Object f) {",
            "      h = f;",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   @Nullable Object h = new Object();",
            "   public void test(@Nullable Object f) {",
            "      h = f;",
            "   }",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Collections.singleton("h")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void fieldNullableArray() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   Object[] h = new Object[4];",
            "   public void test(@Nullable Object[] f) {",
            "      h = f;",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   Object @Nullable [] h = new Object[4];",
            "   public void test(@Nullable Object[] f) {",
            "      h = f;",
            "   }",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Collections.singleton("h")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void addDeclarationAnnotationOnFieldWithTypeAnnotation() {
    injectorTestHelper
        .addInput(
            "Bar.java",
            "package edu.ucr;",
            "import org.hibernate.validator.constraints.NotEmpty;",
            "public class Bar {",
            "   private @NotEmpty String foo;",
            "}")
        .expectOutput(
            "package edu.ucr;",
            "import org.hibernate.validator.constraints.NotEmpty;",
            "import javax.annotation.Nullable;",
            "public class Bar {",
            "   @Nullable private @NotEmpty String foo;",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField("Bar.java", "edu.ucr.Bar", Collections.singleton("foo")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void fieldMultipleAnnotations() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package edu.ucr;",
            "import javax.annotation.Nullable;",
            "public class Test {",
            "   Object h = new Object();",
            "   public void test(@Nullable Object f) {",
            "      h = f;",
            "   }",
            "}")
        .expectOutput(
            "package edu.ucr;",
            "import javax.annotation.Nullable;",
            "import edu.ucr.Tainted;",
            "public class Test {",
            "   @Nullable @Tainted Object h = new Object();",
            "   public void test(@Nullable Object f) {",
            "      h = f;",
            "   }",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField("Foo.java", "edu.ucr.Test", Collections.singleton("h")),
                "javax.annotation.Nullable"),
            new AddMarkerAnnotation(
                new OnField("Foo.java", "edu.ucr.Test", Collections.singleton("h")),
                "edu.ucr.Tainted"),
            new AddMarkerAnnotation(
                new OnField("Foo.java", "edu.ucr.Test", Collections.singleton("h")),
                "javax.annotation.Nullable"))
        .start();
  }
}
