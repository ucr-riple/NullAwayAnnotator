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

package edu.ucr.cs.riple.core;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.singleton;

import edu.ucr.cs.riple.core.tools.TReport;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.injector.location.OnMethod;
import edu.ucr.cs.riple.injector.location.OnParameter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CoreTest extends BaseCoreTest {

  @Test
  public void field() {
    coreTestHelper
        .addInputLines("Main.java", "package test;", "public class Main {", "Object field;", "}")
        .addExpectedReports(
            new TReport(new OnField("Main.java", "test.Main", singleton("field")), -1))
        .start();
  }

  @Test
  public void method() {
    coreTestHelper
        .addInputLines(
            "Main.java",
            "package test;",
            "public class Main {",
            "   Object run() {",
            "     return null;",
            "   }",
            "}")
        .addExpectedReports(new TReport(new OnMethod("Main.java", "test.Main", "run()"), -1))
        .start();
  }

  @Test
  public void param() {
    coreTestHelper
        .addInputLines(
            "Main.java",
            "package test;",
            "public class Main {",
            "   Object run(Object o) {",
            "     return o;",
            "   }",
            "   void pass() {",
            "     run(null);",
            "   }",
            "}")
        .addExpectedReports(
            new TReport(new OnParameter("Main.java", "test.Main", "run(java.lang.Object)", 0), 0))
        .start();
  }

  @Test
  public void param_complete() {
    coreTestHelper
        .addInputLines(
            "Main.java",
            "package test;",
            "public class Main {",
            "   Object run(Object o) {",
            "     return o;",
            "   }",
            "   void pass() {",
            "     run(null);",
            "   }",
            "}")
        .requestCompleteLoop()
        .addExpectedReports(
            new TReport(new OnParameter("Main.java", "test.Main", "run(java.lang.Object)", 0), 0),
            new TReport(new OnMethod("Main.java", "test.Main", "run(java.lang.Object)"), -1))
        .start();
  }

  @Test
  public void field_assign_nullable() {
    coreTestHelper
        .addInputLines(
            "Main.java",
            "package test;",
            "public class Main {",
            "   Object field;",
            "   Main() {",
            "     field = null;",
            "   }",
            "   Object getF(){ return field; }",
            "}")
        .toDepth(1)
        .addExpectedReports(
            new TReport(new OnField("Main.java", "test.Main", singleton("field")), -1))
        .start();
  }

  @Test
  public void field_assign_nullable_constructor() {
    coreTestHelper
        .addInputLines(
            "Main.java",
            "package test;",
            "public class Main {",
            "   Object f;",
            "   Main(Object f) {",
            "     this.f = f;",
            "   }",
            "}",
            "class C {",
            "   Main main = new Main(null);",
            "}")
        .toDepth(1)
        .addExpectedReports(
            new TReport(new OnParameter("Main.java", "test.Main", "Main(java.lang.Object)", 0), 0))
        .start();
  }

  @Test
  public void multiple_field_declaration_test() {
    coreTestHelper
        .addInputDirectory("test", "multiplefielddeclration")
        .disableBailOut()
        .addExpectedReports(
            new TReport(
                new OnField("Main.java", "test.Main", newHashSet("f1", "f2", "f3", "f4")), 9),
            new TReport(new OnField("Main.java", "test.Main", singleton("f5")), 1))
        .toDepth(1)
        .start();
  }

  @Test
  public void inner_class() {
    coreTestHelper
        .addInputDirectory("test", "innerclass")
        .addExpectedReports(
            new TReport(new OnMethod("Main.java", "test.Main$1", "bar(java.lang.Object)"), 0),
            new TReport(new OnMethod("Main.java", "test.Main$1Child", "exec()"), 0),
            new TReport(new OnMethod("Main.java", "test.Main$1Child$1Bar", "returnsNull()"), -1),
            new TReport(new OnField("Main.java", "test.Main$1", singleton("f")), -1))
        .start();
  }

  @Test
  public void multiple_return_nullable() {
    coreTestHelper
        .toDepth(4)
        .addInputDirectory("test", "multiplereturnnullable")
        .disableBailOut()
        .addExpectedReports(
            new TReport(
                new OnParameter("A.java", "test.A", "helper(java.lang.Object)", 0),
                -5,
                newHashSet(
                    new OnParameter("A.java", "test.A", "foo(java.lang.Object)", 0),
                    new OnMethod("A.java", "test.A", "foo(java.lang.Object"),
                    new OnField("A.java", "test.A", singleton("field"))),
                null),
            new TReport(
                new OnParameter("B.java", "test.B", "run(java.lang.Object)", 0),
                -5,
                newHashSet(
                    new OnMethod("B.java", "test.B", "run(java.lang.Object)"),
                    new OnField("B.java", "test.B", singleton("field"))),
                null))
        .start();
  }

  @Test
  public void multiple_return_nullable_recursive() {
    coreTestHelper
        .addInputDirectory("test", "multiplereturnnullablerecursive")
        .addExpectedReports(
            new TReport(new OnField("Main.java", "test.Main", singleton("field")), -6),
            new TReport(new OnMethod("Main.java", "test.Main", "returnNullableRecursive()"), -6),
            new TReport(new OnMethod("Main.java", "test.Main", "returnNullable()"), -6))
        .start();
  }
}
