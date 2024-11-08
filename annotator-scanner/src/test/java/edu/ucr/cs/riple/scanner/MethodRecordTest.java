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

package edu.ucr.cs.riple.scanner;

import com.google.common.base.Preconditions;
import edu.ucr.cs.riple.scanner.tools.DisplayFactory;
import edu.ucr.cs.riple.scanner.tools.MethodRecordDisplay;
import java.util.Arrays;
import org.junit.Test;

public class MethodRecordTest extends AnnotatorScannerBaseTest<MethodRecordDisplay> {

  private static final DisplayFactory<MethodRecordDisplay> METHOD_DISPLAY_FACTORY =
      values -> {
        Preconditions.checkArgument(
            values.length == 9,
            "Expected to find 9 values on each line, but found: "
                + values.length
                + ", "
                + Arrays.toString(values));
        MethodRecordDisplay display =
            new MethodRecordDisplay(
                values[0], values[1], values[2], values[3], values[4], values[5], values[6],
                values[7], values[8]);
        display.uri = display.uri.substring(display.uri.indexOf("edu/ucr/"));
        return display;
      };
  private static final String HEADER =
      String.join(
          "\t",
          "id",
          "class",
          "method",
          "parent",
          "flags",
          "annotations",
          "visibility",
          "non-primitive-return",
          "path");

  private static final String FILE_NAME = "method_records.tsv";

  public MethodRecordTest() {
    super(METHOD_DISPLAY_FACTORY, HEADER, FILE_NAME);
  }

  @Test
  public void basicTest() {
    tester
        .addSourceLines(
            "edu/ucr/A.java",
            "package edu.ucr;",
            "public class A {",
            "   public Object returnNonNull(){",
            "      return new Object();",
            "   }",
            "}")
        .setExpectedOutputs(
            new MethodRecordDisplay(
                "1",
                "edu.ucr.A",
                "returnNonNull()",
                "0",
                "[]",
                "",
                "public",
                "true",
                "edu/ucr/A.java"))
        .doTest();
  }

  @Test
  public void methodIDAssignmentTest() {
    tester
        .addSourceLines(
            "edu/ucr/A.java",
            "package edu.ucr;",
            "public class A {",
            "   public Object returnNonNull(){",
            "      return new Object();",
            "   }",
            "}")
        .addSourceLines(
            "edu/ucr/B.java",
            "package edu.ucr;",
            "public class B extends A{",
            "   @Override",
            "   public Object returnNonNull(){",
            "      return new Object();",
            "   }",
            "}")
        .setExpectedOutputs(
            new MethodRecordDisplay(
                "1",
                "edu.ucr.A",
                "returnNonNull()",
                "0",
                "[]",
                "",
                "public",
                "true",
                "edu/ucr/A.java"),
            new MethodRecordDisplay(
                "2",
                "edu.ucr.B",
                "returnNonNull()",
                "1",
                "[]",
                "java.lang.Override",
                "public",
                "true",
                "edu/ucr/B.java"))
        .doTest();
  }

  @Test
  public void visibilityTest() {
    tester
        .addSourceLines(
            "edu/ucr/A.java",
            "package edu.ucr;",
            "public class A {",
            "   public Object publicMethod(){",
            "      return new Object();",
            "   }",
            "   private Object privateMethod(){",
            "      return new Object();",
            "   }",
            "   protected Object protectedMethod(){",
            "      return new Object();",
            "   }",
            "   Object packageMethod(){",
            "      return new Object();",
            "   }",
            "}")
        .setExpectedOutputs(
            new MethodRecordDisplay(
                "1",
                "edu.ucr.A",
                "publicMethod()",
                "0",
                "[]",
                "",
                "public",
                "true",
                "edu/ucr/A.java"),
            new MethodRecordDisplay(
                "2",
                "edu.ucr.A",
                "privateMethod()",
                "0",
                "[]",
                "",
                "private",
                "true",
                "edu/ucr/A.java"),
            new MethodRecordDisplay(
                "3",
                "edu.ucr.A",
                "protectedMethod()",
                "0",
                "[]",
                "",
                "protected",
                "true",
                "edu/ucr/A.java"),
            new MethodRecordDisplay(
                "4",
                "edu.ucr.A",
                "packageMethod()",
                "0",
                "[]",
                "",
                "package",
                "true",
                "edu/ucr/A.java"))
        .doTest();
  }

  @Test
  public void visibilityAndReturnTypeTest() {
    tester
        .addSourceLines(
            "edu/ucr/A.java",
            "package edu.ucr;",
            "public abstract class A {",
            "   static Object publicMethod(){",
            "      return new Object();",
            "   }",
            "   public abstract Object publicAbstractMethod();",
            "}")
        .addSourceLines(
            "edu/ucr/B.java",
            "package edu.ucr;",
            "import javax.annotation.Nullable;",
            "public interface B {",
            "   void foo();",
            "   @Nullable",
            "   default Object run() {",
            "       return null;",
            "   }",
            "}")
        .setExpectedOutputs(
            new MethodRecordDisplay(
                "1",
                "edu.ucr.A",
                "publicMethod()",
                "0",
                "[]",
                "",
                "package",
                "true",
                "edu/ucr/A.java"),
            new MethodRecordDisplay(
                "2",
                "edu.ucr.A",
                "publicAbstractMethod()",
                "0",
                "[]",
                "",
                "public",
                "true",
                "edu/ucr/A.java"),
            new MethodRecordDisplay(
                "3", "edu.ucr.B", "foo()", "0", "[]", "", "public", "false", "edu/ucr/B.java"),
            new MethodRecordDisplay(
                "4",
                "edu.ucr.B",
                "run()",
                "0",
                "[]",
                "javax.annotation.Nullable",
                "public",
                "true",
                "edu/ucr/B.java"))
        .doTest();
  }

  @Test
  public void lombokGeneratedAnnotationsOnMethodTest() {
    tester
        .addSourceLines(
            "lombok/Generated.java", "package lombok;", "public @interface Generated { }")
        .addSourceLines(
            "edu/ucr/A.java",
            "package edu.ucr;",
            "import lombok.Generated;",
            "public class A {",
            "   Object foo;",
            "   @lombok.Generated()",
            "   @SuppressWarnings({\"Check1\",\"Check2\"})",
            "   public Object bar(){",
            "       return foo;",
            "   }",
            "}")
        .setExpectedOutputs(
            new MethodRecordDisplay(
                "1",
                "edu.ucr.A",
                "bar()",
                "0",
                "[]",
                "lombok.Generated,java.lang.SuppressWarnings",
                "public",
                "true",
                "edu/ucr/A.java"))
        .doTest();
  }

  @Test
  public void typeUseAnnotationSerializationTest() {
    tester
        .addSourceLines(
            // Exact copy of org.jspecify.annotations.Nullable
            "org/jspecify/annotations/Nullable.java",
            "package org.jspecify.annotations;",
            "import static java.lang.annotation.ElementType.TYPE_USE;",
            "import static java.lang.annotation.RetentionPolicy.RUNTIME;",
            "import java.lang.annotation.Documented;",
            "import java.lang.annotation.Retention;",
            "import java.lang.annotation.Target;",
            "@Documented",
            "@Target(TYPE_USE)",
            "@Retention(RUNTIME)",
            "public @interface Nullable {}")
        .addSourceLines(
            "edu/ucr/A.java",
            "package edu.ucr;",
            "import org.jspecify.annotations.Nullable;",
            "public class A {",
            "   @javax.annotation.Nullable",
            "   public Object bar(){",
            "       return null;",
            "   }",
            "   public @Nullable Object foo(){",
            "       return null;",
            "   }",
            "}")
        .setExpectedOutputs(
            new MethodRecordDisplay(
                "1",
                "edu.ucr.A",
                "bar()",
                "0",
                "[]",
                "javax.annotation.Nullable",
                "public",
                "true",
                "edu/ucr/A.java"),
            new MethodRecordDisplay(
                "2",
                "edu.ucr.A",
                "foo()",
                "0",
                "[]",
                "org.jspecify.annotations.Nullable",
                "public",
                "true",
                "edu/ucr/A.java"))
        .doTest();
  }
}
