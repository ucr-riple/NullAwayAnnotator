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

import static edu.ucr.cs.riple.core.AnalysisMode.LOWER_BOUND;
import static edu.ucr.cs.riple.core.AnalysisMode.STRICT;
import static edu.ucr.cs.riple.core.Report.Tag.APPLY;
import static edu.ucr.cs.riple.core.Report.Tag.DISCARD;

import edu.ucr.cs.riple.core.tools.TReport;
import edu.ucr.cs.riple.injector.location.OnMethod;
import edu.ucr.cs.riple.injector.location.OnParameter;
import java.util.List;
import java.util.Objects;
import org.junit.Test;

public class AnalysisModeTest extends BaseCoreTest {

  public AnalysisModeTest() {
    super("analysis-mode-test", List.of("Target", "DepA", "DepB", "DepC"));
  }

  @Test
  public void strictModeTest() {
    coreTestHelper
        .addExpectedReports(
            new TReport(new OnMethod("Foo.java", "test.target.Foo", "returnNullGood()"), -4, APPLY),
            new TReport(
                new OnMethod("Foo.java", "test.target.Foo", "returnNullBad()"), -4, DISCARD),
            new TReport(
                new OnParameter("Foo.java", "test.target.Foo", "takeNullGood(java.lang.Object)", 0),
                0,
                APPLY),
            new TReport(
                new OnParameter("Foo.java", "test.target.Foo", "takeNullBad(java.lang.Object)", 0),
                1,
                APPLY))
        .setPredicate(
            (expected, found) ->
                expected.root.equals(found.root)
                    && expected.getExpectedValue()
                        == found.getOverallEffect(coreTestHelper.getConfig())
                    && Objects.equals(expected.getExpectedTag(), found.getTag()))
        .toDepth(1)
        .requestCompleteLoop()
        .enableDownstreamDependencyAnalysis(STRICT)
        .start();
  }

  @Test
  public void lowerBoundModeTest() {
    coreTestHelper
        .addExpectedReports(
            new TReport(new OnMethod("Foo.java", "test.target.Foo", "returnNullGood()"), -4, APPLY),
            new TReport(new OnMethod("Foo.java", "test.target.Foo", "returnNullBad()"), -4, APPLY),
            new TReport(
                new OnParameter("Foo.java", "test.target.Foo", "takeNullGood(java.lang.Object)", 0),
                0,
                APPLY),
            new TReport(
                new OnParameter("Foo.java", "test.target.Foo", "takeNullBad(java.lang.Object)", 0),
                1,
                DISCARD))
        .setPredicate(
            (expected, found) ->
                expected.root.equals(found.root)
                    && expected.getExpectedValue()
                        == found.getOverallEffect(coreTestHelper.getConfig())
                    && Objects.equals(expected.getExpectedTag(), found.getTag()))
        .toDepth(1)
        .requestCompleteLoop()
        .enableDownstreamDependencyAnalysis(LOWER_BOUND)
        .start();
  }
}
