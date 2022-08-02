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

public class InheritanceTest extends BaseCoreTest {

  public InheritanceTest() {
    super("unittest", singleton("unittest"));
  }

  @Test
  public void basic_method() {
    coreTestHelper
        .addInputLines(
            "Base.java",
            "package test;",
            "public class Base {",
            "   public Object foo(){ return null; }",
            "}")
        .addInputLines(
            "Child.java",
            "package test;",
            "public class Child extends Base {",
            "   public Object foo(){ return null; }",
            "}")
        .disableBailOut()
        .toDepth(2)
        .addExpectedReports(
            new TReport(
                new OnMethod("Child.java", "test.Child", "foo()"),
                -2,
                newHashSet(new OnMethod("Base.java", "test.Base", "foo()")),
                null),
            new TReport(new OnMethod("Base.java", "test.Base", "foo()"), -1))
        .start();
  }

  @Test
  public void builder() {
    coreTestHelper
        .addInputDirectory("test", "builder")
        .requestCompleteLoop()
        .setPredicate(Report::testEquals)
        .addExpectedReports(
            new TReport(new OnField("A.java", "test.A", singleton("arg3")), -1),
            new TReport(new OnField("A.java", "test.A", singleton("arg2")), -1),
            new TReport(new OnField("B.java", "test.B", singleton("body")), -1),
            new TReport(
                new OnField("A.java", "test.A$Builder", singleton("arg2")),
                0,
                null,
                singleton(new OnField("A.java", "test.A", singleton("arg2")))),
            new TReport(
                new OnField("B.java", "test.B$Builder", singleton("body")),
                0,
                null,
                singleton(new OnField("B.java", "test.B", singleton("body")))),
            new TReport(
                new OnField("A.java", "test.A$Builder", singleton("arg3")),
                0,
                null,
                singleton(new OnField("A.java", "test.A", singleton("arg3")))),
            new TReport(
                new OnParameter("A.java", "test.A$Builder", "setArg2(test.Y)", 0),
                -3,
                newHashSet(
                    new OnParameter("C.java", "test.C$Builder", "setArg2(test.Y)", 0),
                    new OnParameter("B.java", "test.B$Builder", "setArg2(test.Y)", 0),
                    new OnParameter("E.java", "test.E$Builder", "setArg2(test.Y)", 0),
                    new OnParameter("D.java", "test.D$Builder", "setArg2(test.Y)", 0),
                    new OnField("A.java", "test.A$Builder", singleton("arg2")),
                    new OnField("A.java", "test.A", singleton("arg2"))),
                null))
        .toDepth(5)
        .start();
  }

  @Test
  public void parameter_track() {
    coreTestHelper
        .addInputDirectory("test", "parametertrack")
        .setPredicate(Report::testEquals)
        .disableBailOut()
        .toDepth(10)
        .addExpectedReports(
            new TReport(
                new OnParameter("Base.java", "test.Base", "run(java.lang.Object)", 0),
                0,
                newHashSet(
                    new OnMethod("Base.java", "test.Base", "run(java.lang.Object)"),
                    new OnField("Base.java", "test.Base", singleton("field")),
                    new OnParameter("Child.java", "test.Child", "run(java.lang.Object)", 0),
                    new OnParameter(
                        "GrandChild.java", "test.GrandChild", "run(java.lang.Object)", 0)),
                null))
        .start();
  }

  @Test
  public void method_track() {
    coreTestHelper
        .addInputDirectory("test", "methodtrack")
        .setPredicate(Report::testEquals)
        .disableBailOut()
        .toDepth(10)
        .addExpectedReports(
            new TReport(
                new OnParameter("B.java", "test.B", "helper(java.lang.Object)", 0),
                -1,
                newHashSet(
                    new OnParameter("B.java", "test.B", "run(java.lang.Object)", 0),
                    new OnMethod("B.java", "test.B", "run(java.lang.Object)"),
                    new OnParameter("A.java", "test.A", "run(java.lang.Object)", 0),
                    new OnMethod("A.java", "test.A", "run(java.lang.Object)"),
                    new OnField("A.java", "test.A", singleton("foo")),
                    new OnMethod("Root.java", "test.Root", "run(java.lang.Object)"),
                    new OnParameter("C.java", "test.C", "run(java.lang.Object)", 0),
                    new OnMethod("C.java", "test.C", "run(java.lang.Object)"),
                    new OnParameter("D.java", "test.D", "run(java.lang.Object)", 0),
                    new OnMethod("D.java", "test.D", "run(java.lang.Object)"),
                    new OnParameter("E.java", "test.E", "run(java.lang.Object)", 0),
                    new OnMethod("E.java", "test.E", "run(java.lang.Object)")),
                null))
        .start();
  }
}
