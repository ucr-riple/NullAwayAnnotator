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

import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.ucr.cs.riple.injector.changes.RemoveMarkerAnnotation;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.injector.location.OnMethod;
import edu.ucr.cs.riple.injector.location.OnParameter;
import java.util.Collections;
import org.junit.Test;

public class RemovalTest extends BaseInjectorTest {

  @Test
  public void removeAnnotReturnNullable() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   @Nullable Object test(@javax.annotation.Nullable Object o) {",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   Object test(@javax.annotation.Nullable Object o) {",
            "   }",
            "}")
        .addChanges(
            new RemoveMarkerAnnotation(
                new OnMethod("Foo.java", "test.Foo", "test(java.lang.Object)"),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void removeAnnotParam() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   @Nullable Object test(@Nullable Object o) {",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   @Nullable Object test(Object o) {",
            "   }",
            "}")
        .addChanges(
            new RemoveMarkerAnnotation(
                new OnParameter("Foo.java", "test.Foo", "test(java.lang.Object)", 0),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void removeAnnotParamFullName() {
    // Injector should not remove annotations that are present with fully qualified name. This test
    // is designed to alert us if we decide we would like injector to remove these too in the
    // future.
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   @Nullable Object test(@javax.annotation.Nullable Object o) {",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   @Nullable Object test(Object o) {",
            "   }",
            "}")
        .addChanges(
            new RemoveMarkerAnnotation(
                new OnParameter("Foo.java", "test.Foo", "test(java.lang.Object)", 0),
                "javax.annotation.Nullable"));
    assertThrows(AssertionError.class, () -> injectorTestHelper.start());
  }

  @Test
  public void removeAnnotField() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   @Nullable Object f;",
            "   @Nullable Object test(@javax.annotation.Nullable Object o) {",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   Object f;",
            "   @Nullable Object test(@javax.annotation.Nullable Object o) {",
            "   }",
            "}")
        .addChanges(
            new RemoveMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Collections.singleton("f")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void removeMethodInlineAnnot() {
    injectorTestHelper
        .addInput(
            "Test.java",
            "package edu.ucr;",
            "import edu.custom.Annot;",
            "public class Test {",
            "   public @Annot Object test() {",
            "   }",
            "}")
        .expectOutput(
            "package edu.ucr;",
            "import edu.custom.Annot;",
            "public class Test {",
            "   public Object test() {",
            "   }",
            "}")
        .addChanges(
            new RemoveMarkerAnnotation(
                new OnMethod("Test.java", "edu.ucr.Test", "test()"), "edu.custom.Annot"))
        .start();
  }

  @Test
  public void removeParamAnnotEndingWithUnderlineExists() {
    injectorTestHelper
        .addInput(
            "Test.java",
            "package edu.ucr;",
            "import javax.annotation.Nullable;",
            "import com.customAnnot.AnnotationWithNonAlphanumericSuffix$$$$_;",
            "public class Test {",
            "   public void foo(@AnnotationWithNonAlphanumericSuffix$$$$_ @Nullable Object o) { }",
            "}")
        .expectOutput(
            "package edu.ucr;",
            "import javax.annotation.Nullable;",
            "import com.customAnnot.AnnotationWithNonAlphanumericSuffix$$$$_;",
            "public class Test {",
            "   public void foo(@AnnotationWithNonAlphanumericSuffix$$$$_ Object o) { }",
            "}")
        .addChanges(
            new RemoveMarkerAnnotation(
                new OnParameter("Test.java", "edu.ucr.Test", "foo(java.lang.Object)", 0),
                "javax.annotation.Nullable"))
        .start();
  }
}
