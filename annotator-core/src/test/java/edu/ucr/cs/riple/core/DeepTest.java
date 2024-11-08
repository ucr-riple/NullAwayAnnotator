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

package edu.ucr.cs.riple.core;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.singleton;

import edu.ucr.cs.riple.core.tools.TReport;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.injector.location.OnMethod;
import edu.ucr.cs.riple.injector.location.OnParameter;
import java.util.Collections;
import org.junit.Test;

public class DeepTest extends AnnotatorBaseCoreTest {

  public DeepTest() {
    super("nullable-multi-modular");
  }

  @Test
  public void fieldAssignNullableBailoutEnabled() {
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Main.java",
            "package test;",
            "public class Main {",
            "   Object field;",
            "   Main() {",
            "     field = null;",
            "   }",
            "   Object getF(){ return field;}",
            "}")
        .withExpectedReports(
            new TReport(new OnField("Main.java", "test.Main", Collections.singleton("field")), -1))
        .toDepth(2)
        .start();
  }

  @Test
  public void fieldAssignNullableBailoutDisabled() {
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Main.java",
            "package test;",
            "public class Main {",
            "   Object field;",
            "   Main() {",
            "     field = null;",
            "   }",
            "   Object getF(){ return field;}",
            "}")
        .withExpectedReports(
            new TReport(
                new OnField("Main.java", "test.Main", Collections.singleton("field")),
                -2,
                null,
                singleton(new OnMethod("Main.java", "test.Main", "getF()"))))
        .toDepth(2)
        .disableBailOut()
        .start();
  }

  @Test
  public void testInitializationErrorForMultipleFields() {
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Main.java",
            "package test;",
            "public class Main {",
            "   Object field1;",
            "   Object field2;",
            "   Main() {",
            "     field1 = null;",
            "     field2 = null;",
            "   }",
            "   Object getF1(){ return field1; }",
            "   Object getF2(){ return field2; }",
            "}")
        .withExpectedReports(
            new TReport(
                new OnField("Main.java", "test.Main", Collections.singleton("field1")),
                -1,
                singleton(new OnMethod("Main.java", "test.Main", "getF1()")),
                null),
            new TReport(
                new OnField("Main.java", "test.Main", Collections.singleton("field2")),
                -1,
                singleton(new OnMethod("Main.java", "test.Main", "getF2()")),
                null))
        .toDepth(2)
        .disableBailOut()
        .setPredicate((expected, found) -> expected.testEquals(coreTestHelper.getConfig(), found))
        .start();
  }

  @Test
  public void paramPassTest() {
    coreTestHelper
        .onTarget()
        .withSourceDirectory("test", "parampass")
        .withExpectedReports(
            new TReport(
                new OnField("Main.java", "test.Main", singleton("f")),
                -1,
                newHashSet(
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "f_param(java.lang.Object,java.lang.Object,java.lang.Object)",
                        0),
                    new OnMethod("Main.java", "test.Main", "f_run()"),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "f_param1(java.lang.Object,java.lang.Object,java.lang.Object)",
                        0),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "f_param2(java.lang.Object,java.lang.Object,java.lang.Object)",
                        0),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "f_param3(java.lang.Object,java.lang.Object,java.lang.Object)",
                        0),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "f_param4(java.lang.Object,java.lang.Object,java.lang.Object)",
                        0),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "f_param5(java.lang.Object,java.lang.Object,java.lang.Object)",
                        0),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "f_param6(java.lang.Object,java.lang.Object,java.lang.Object)",
                        0),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "d_param(java.lang.Object,java.lang.Object,java.lang.Object)",
                        0),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "d_param1(java.lang.Object,java.lang.Object,java.lang.Object)",
                        0),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "d_param2(java.lang.Object,java.lang.Object,java.lang.Object)",
                        0),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "d_param3(java.lang.Object,java.lang.Object,java.lang.Object)",
                        0),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "d_param4(java.lang.Object,java.lang.Object,java.lang.Object)",
                        0),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "d_param5(java.lang.Object,java.lang.Object,java.lang.Object)",
                        0),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "d_param6(java.lang.Object,java.lang.Object,java.lang.Object)",
                        0),
                    new OnField("Other.java", "test.Other", singleton("f")),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "f_param(java.lang.Object,java.lang.Object,java.lang.Object)",
                        0),
                    new OnMethod("Other.java", "test.Other", "f_run()"),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "f_param1(java.lang.Object,java.lang.Object,java.lang.Object)",
                        0),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "f_param2(java.lang.Object,java.lang.Object,java.lang.Object)",
                        0),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "f_param3(java.lang.Object,java.lang.Object,java.lang.Object)",
                        0),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "f_param4(java.lang.Object,java.lang.Object,java.lang.Object)",
                        0),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "f_param5(java.lang.Object,java.lang.Object,java.lang.Object)",
                        0),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "f_param6(java.lang.Object,java.lang.Object,java.lang.Object)",
                        0),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "d_param(java.lang.Object,java.lang.Object,java.lang.Object)",
                        0),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "d_param1(java.lang.Object,java.lang.Object,java.lang.Object)",
                        0),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "d_param2(java.lang.Object,java.lang.Object,java.lang.Object)",
                        0),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "d_param3(java.lang.Object,java.lang.Object,java.lang.Object)",
                        0),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "d_param4(java.lang.Object,java.lang.Object,java.lang.Object)",
                        0),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "d_param5(java.lang.Object,java.lang.Object,java.lang.Object)",
                        0),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "d_param6(java.lang.Object,java.lang.Object,java.lang.Object)",
                        0)),
                null),
            new TReport(
                new OnField("Main.java", "test.Main", singleton("d")),
                -1,
                newHashSet(
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "f_param(java.lang.Object,java.lang.Object,java.lang.Object)",
                        1),
                    new OnMethod("Main.java", "test.Main", "d_run()"),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "f_param1(java.lang.Object,java.lang.Object,java.lang.Object)",
                        1),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "f_param2(java.lang.Object,java.lang.Object,java.lang.Object)",
                        1),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "f_param3(java.lang.Object,java.lang.Object,java.lang.Object)",
                        1),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "f_param4(java.lang.Object,java.lang.Object,java.lang.Object)",
                        1),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "f_param5(java.lang.Object,java.lang.Object,java.lang.Object)",
                        1),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "f_param6(java.lang.Object,java.lang.Object,java.lang.Object)",
                        1),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "d_param(java.lang.Object,java.lang.Object,java.lang.Object)",
                        1),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "d_param1(java.lang.Object,java.lang.Object,java.lang.Object)",
                        1),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "d_param2(java.lang.Object,java.lang.Object,java.lang.Object)",
                        1),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "d_param3(java.lang.Object,java.lang.Object,java.lang.Object)",
                        1),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "d_param4(java.lang.Object,java.lang.Object,java.lang.Object)",
                        1),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "d_param5(java.lang.Object,java.lang.Object,java.lang.Object)",
                        1),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "d_param6(java.lang.Object,java.lang.Object,java.lang.Object)",
                        1),
                    new OnField("Other.java", "test.Other", singleton("d")),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "f_param(java.lang.Object,java.lang.Object,java.lang.Object)",
                        1),
                    new OnMethod("Other.java", "test.Other", "d_run()"),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "f_param1(java.lang.Object,java.lang.Object,java.lang.Object)",
                        1),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "f_param2(java.lang.Object,java.lang.Object,java.lang.Object)",
                        1),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "f_param3(java.lang.Object,java.lang.Object,java.lang.Object)",
                        1),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "f_param4(java.lang.Object,java.lang.Object,java.lang.Object)",
                        1),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "f_param5(java.lang.Object,java.lang.Object,java.lang.Object)",
                        1),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "f_param6(java.lang.Object,java.lang.Object,java.lang.Object)",
                        1),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "d_param(java.lang.Object,java.lang.Object,java.lang.Object)",
                        1),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "d_param1(java.lang.Object,java.lang.Object,java.lang.Object)",
                        1),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "d_param2(java.lang.Object,java.lang.Object,java.lang.Object)",
                        1),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "d_param3(java.lang.Object,java.lang.Object,java.lang.Object)",
                        1),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "d_param4(java.lang.Object,java.lang.Object,java.lang.Object)",
                        1),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "d_param5(java.lang.Object,java.lang.Object,java.lang.Object)",
                        1),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "d_param6(java.lang.Object,java.lang.Object,java.lang.Object)",
                        1)),
                null),
            new TReport(
                new OnField("Main.java", "test.Main", singleton("p")),
                -1,
                newHashSet(
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "f_param(java.lang.Object,java.lang.Object,java.lang.Object)",
                        2),
                    new OnMethod("Main.java", "test.Main", "p_run()"),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "f_param1(java.lang.Object,java.lang.Object,java.lang.Object)",
                        2),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "f_param2(java.lang.Object,java.lang.Object,java.lang.Object)",
                        2),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "f_param3(java.lang.Object,java.lang.Object,java.lang.Object)",
                        2),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "f_param4(java.lang.Object,java.lang.Object,java.lang.Object)",
                        2),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "f_param5(java.lang.Object,java.lang.Object,java.lang.Object)",
                        2),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "f_param6(java.lang.Object,java.lang.Object,java.lang.Object)",
                        2),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "d_param(java.lang.Object,java.lang.Object,java.lang.Object)",
                        2),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "d_param1(java.lang.Object,java.lang.Object,java.lang.Object)",
                        2),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "d_param2(java.lang.Object,java.lang.Object,java.lang.Object)",
                        2),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "d_param3(java.lang.Object,java.lang.Object,java.lang.Object)",
                        2),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "d_param4(java.lang.Object,java.lang.Object,java.lang.Object)",
                        2),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "d_param5(java.lang.Object,java.lang.Object,java.lang.Object)",
                        2),
                    new OnParameter(
                        "Main.java",
                        "test.Main",
                        "d_param6(java.lang.Object,java.lang.Object,java.lang.Object)",
                        2),
                    new OnField("Other.java", "test.Other", singleton("p")),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "f_param(java.lang.Object,java.lang.Object,java.lang.Object)",
                        2),
                    new OnMethod("Other.java", "test.Other", "p_run()"),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "f_param1(java.lang.Object,java.lang.Object,java.lang.Object)",
                        2),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "f_param2(java.lang.Object,java.lang.Object,java.lang.Object)",
                        2),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "f_param3(java.lang.Object,java.lang.Object,java.lang.Object)",
                        2),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "f_param4(java.lang.Object,java.lang.Object,java.lang.Object)",
                        2),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "f_param5(java.lang.Object,java.lang.Object,java.lang.Object)",
                        2),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "f_param6(java.lang.Object,java.lang.Object,java.lang.Object)",
                        2),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "d_param(java.lang.Object,java.lang.Object,java.lang.Object)",
                        2),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "d_param1(java.lang.Object,java.lang.Object,java.lang.Object)",
                        2),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "d_param2(java.lang.Object,java.lang.Object,java.lang.Object)",
                        2),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "d_param3(java.lang.Object,java.lang.Object,java.lang.Object)",
                        2),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "d_param4(java.lang.Object,java.lang.Object,java.lang.Object)",
                        2),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "d_param5(java.lang.Object,java.lang.Object,java.lang.Object)",
                        2),
                    new OnParameter(
                        "Other.java",
                        "test.Other",
                        "d_param6(java.lang.Object,java.lang.Object,java.lang.Object)",
                        2)),
                null))
        .setPredicate((expected, found) -> expected.testEquals(coreTestHelper.getConfig(), found))
        .toDepth(10)
        .start();
  }
}
