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

import static java.util.Collections.singleton;

import com.google.common.collect.Sets;
import edu.ucr.cs.riple.core.tools.TReport;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.injector.location.OnParameter;
import org.junit.Test;

public class InheritanceTest extends BaseCoreTest {

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
                -1,
                Sets.newHashSet(
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
}
