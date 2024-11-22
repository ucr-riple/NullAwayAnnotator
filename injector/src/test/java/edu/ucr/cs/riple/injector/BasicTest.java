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

import edu.ucr.cs.riple.injector.changes.ASTChange;
import edu.ucr.cs.riple.injector.changes.AddMarkerAnnotation;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.injector.location.OnMethod;
import java.util.Collections;
import org.junit.Test;

public class BasicTest extends BaseInjectorTest {

  @Test
  public void skipDuplicateAnnotation() {
    ASTChange change1 =
        new AddMarkerAnnotation(
            new OnMethod("Foo.java", "test.Foo", "test()"), "javax.annotation.Nullable");
    ASTChange change2 =
        new AddMarkerAnnotation(
            new OnMethod("Foo.java", "test.Foo", "test()"), "javax.annotation.Nullable");
    ASTChange change3 =
        new AddMarkerAnnotation(
            new OnMethod("Foo.java", "test.Foo", "test()"), "javax.annotation.Nullable");

    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   @Nullable Object test() {",
            "       return new Object();",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   @Nullable Object test() {",
            "       return new Object();",
            "   }",
            "}")
        .addChanges(change1, change2, change3)
        .start();
  }

  @Test
  public void skipExistingAnnotations() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   Object test(@javax.annotation.Nullable Object o) {",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   @Nullable Object test(@javax.annotation.Nullable Object o) {",
            "   }",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnMethod("Foo.java", "test.Foo", "test(java.lang.Object)"),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void ignoreCommentsOnAnnotationEqualCheckTest() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package edu.ucr;",
            "import javax.annotation.Nullable;",
            "public class Test {",
            "  @Nullable @GuardedBy(\"this\") // Either a RuntimeException, non-fatal Error, or IOException.",
            "  private Throwable creationFailure;",
            "}")
        .expectOutput(
            "package edu.ucr;",
            "import javax.annotation.Nullable;",
            "public class Test {",
            "  @Nullable @GuardedBy(\"this\") // Either a RuntimeException, non-fatal Error, or IOException.",
            "  private Throwable creationFailure;",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField("Foo.java", "edu.ucr.Test", Collections.singleton("creationFailure")),
                "javax.annotation.Nullable"))
        .start();
  }
}
