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

package edu.ucr.cs.riple.core;

import org.junit.Test;

public class CodeFixTest extends AnnotatorBaseCoreTest {

  public CodeFixTest() {
    super("nullable-multi-modular");
  }

  @Test
  public void dereferenceEqualsRewriteTest() {
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Foo.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   @Nullable Object f;",
            "   public boolean equals(Object other) {",
            "      if(other == null) {",
            "         return false;",
            "      }",
            "      if(other instanceof Foo) {",
            "         return f.equals(((Foo) other).f);",
            "      }",
            "      return false;",
            "   }",
            "}")
        .expectNoReport()
        .resolveRemainingErrors()
        .start();
  }

  @Test
  public void dereferenceHashCodeRewriteTest() {
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Foo.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   @Nullable Object f;",
            "   public int hashCode() {",
            "      return f.hashCode();",
            "   }",
            "}")
        .expectNoReport()
        .resolveRemainingErrors()
        .start();
  }

  @Test
  public void dereferenceToStringRewriteTest() {
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Foo.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   @Nullable Object f;",
            "   public String toString() {",
            "      return f.toString();",
            "   }",
            "}")
        .expectNoReport()
        .resolveRemainingErrors()
        .start();
  }

  @Test
  public void castToNullDereferenceTest() {
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Foo.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "import java.util.Collection;",
            "public class Foo {",
            "   public String toString(@Nullable Collection<?> coll) {",
            "     boolean isEmpty = coll == null || coll.isEmpty();",
            "     if(isEmpty) {",
            "       return \"\";",
            "     }",
            "     return coll.iterator().next().toString();",
            "   }",
            "}")
        .expectNoReport()
        .resolveRemainingErrors()
        .start();
  }

  @Test
  public void returnNullForNullableExpressionInNullableMethodTest() {
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Foo.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "import java.util.Collection;",
            "public class Foo {",
            "   public @Nullable String foo(@Nullable Collection<?> coll) {",
            "     return coll.iterator().next().toString();",
            "   }",
            "}")
        .expectNoReport()
        .resolveRemainingErrors()
        .start();
  }
}
