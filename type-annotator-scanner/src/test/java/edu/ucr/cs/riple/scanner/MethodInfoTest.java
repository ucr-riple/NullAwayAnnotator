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
import edu.ucr.cs.riple.scanner.tools.MethodInfoDisplay;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MethodInfoTest extends TypeAnnotatorScannerBaseTest<MethodInfoDisplay> {

  private static final DisplayFactory<MethodInfoDisplay> METHOD_DISPLAY_FACTORY =
      values -> {
        Preconditions.checkArgument(
            values.length == 10,
            "Expected to find 10 values on each line, but found: "
                + values.length
                + ", "
                + Arrays.toString(values));
        MethodInfoDisplay display =
            new MethodInfoDisplay(
                values[0], values[1], values[2], values[3], values[4], values[5], values[6],
                values[7], values[8], values[9]);
        display.uri = display.uri.substring(display.uri.indexOf("edu/ucr/"));
        return display;
      };
  private static final String HEADER =
      "id"
          + "\t"
          + "class"
          + "\t"
          + "method"
          + "\t"
          + "parent"
          + "\t"
          + "size"
          + "\t"
          + "flags"
          + "\t"
          + "nullable"
          + "\t"
          + "visibility"
          + "\t"
          + "non-primitive-return"
          + "\t"
          + "uri";
  private static final String FILE_NAME = "method_info.tsv";

  public MethodInfoTest() {
    super(METHOD_DISPLAY_FACTORY, HEADER, FILE_NAME);
  }

  @Test
  public void BasicTest() {
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
            new MethodInfoDisplay(
                "1",
                "edu.ucr.A",
                "returnNonNull()",
                "0",
                "0",
                "[]",
                "false",
                "public",
                "true",
                "edu/ucr/A.java"))
        .doTest();
  }

  @Test
  public void TestID() {
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
            "   public Object returnNonNull(){",
            "      return new Object();",
            "   }",
            "}")
        .setExpectedOutputs(
            new MethodInfoDisplay(
                "1",
                "edu.ucr.A",
                "returnNonNull()",
                "0",
                "0",
                "[]",
                "false",
                "public",
                "true",
                "edu/ucr/A.java"),
            new MethodInfoDisplay(
                "2",
                "edu.ucr.B",
                "returnNonNull()",
                "1",
                "0",
                "[]",
                "false",
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
            new MethodInfoDisplay(
                "1",
                "edu.ucr.A",
                "publicMethod()",
                "0",
                "0",
                "[]",
                "false",
                "public",
                "true",
                "edu/ucr/A.java"),
            new MethodInfoDisplay(
                "2",
                "edu.ucr.A",
                "privateMethod()",
                "0",
                "0",
                "[]",
                "false",
                "private",
                "true",
                "edu/ucr/A.java"),
            new MethodInfoDisplay(
                "3",
                "edu.ucr.A",
                "protectedMethod()",
                "0",
                "0",
                "[]",
                "false",
                "protected",
                "true",
                "edu/ucr/A.java"),
            new MethodInfoDisplay(
                "4",
                "edu.ucr.A",
                "packageMethod()",
                "0",
                "0",
                "[]",
                "false",
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
            new MethodInfoDisplay(
                "1",
                "edu.ucr.A",
                "publicMethod()",
                "0",
                "0",
                "[]",
                "false",
                "package",
                "true",
                "edu/ucr/A.java"),
            new MethodInfoDisplay(
                "2",
                "edu.ucr.A",
                "publicAbstractMethod()",
                "0",
                "0",
                "[]",
                "false",
                "public",
                "true",
                "edu/ucr/A.java"),
            new MethodInfoDisplay(
                "3",
                "edu.ucr.B",
                "foo()",
                "0",
                "0",
                "[]",
                "false",
                "public",
                "false",
                "edu/ucr/B.java"),
            new MethodInfoDisplay(
                "4",
                "edu.ucr.B",
                "run()",
                "0",
                "0",
                "[]",
                "true",
                "public",
                "true",
                "edu/ucr/B.java"))
        .doTest();
  }
}
