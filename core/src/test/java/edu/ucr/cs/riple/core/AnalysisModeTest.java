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
import static edu.ucr.cs.riple.core.AnalysisMode.UPPER_BOUND;
import static edu.ucr.cs.riple.core.Report.Tag.APPROVE;
import static edu.ucr.cs.riple.core.Report.Tag.REJECT;

import edu.ucr.cs.riple.core.tools.TReport;
import edu.ucr.cs.riple.injector.location.OnMethod;
import java.util.List;
import java.util.Objects;
import org.junit.Test;

public class AnalysisModeTest extends BaseCoreTest {

  public AnalysisModeTest() {
    super("analysis-mode-test", List.of("Target", "Dep"));
  }

  @Test
  public void strictModeTest() {
    coreTestHelper
        .addExpectedReports(
            // Resolves 6 errors locally and all errors on downstream dependencies can be resolved
            // with no new triggered error, therefore the effect is -6. The fix triggers no error
            // in downstream dependencies, should be approved.
            new TReport(
                new OnMethod("Foo.java", "test.target.Foo", "returnNullGood()"), -6, APPROVE),
            // Resolves 6 errors locally but creates 1 error on downstream dependencies that cannot
            // be resolved, therefore the effect is -5. The fix triggers 1 error in downstream
            // dependencies, should be rejected.
            new TReport(new OnMethod("Foo.java", "test.target.Foo", "returnNullBad()"), -5, REJECT))
        .setPredicate(
            (expected, found) ->
                expected.root.equals(found.root)
                    && expected.getExpectedValue()
                        == found.getOverallEffect(coreTestHelper.getConfig())
                    && Objects.equals(expected.getTag(), found.getTag()))
        .toDepth(5)
        .disableBailOut()
        .enableDownstreamDependencyAnalysis(STRICT)
        .start();
  }

  @Test
  public void lowerBoundModeTest() {
    coreTestHelper
        .addExpectedReports(
            new TReport(
                new OnMethod("Foo.java", "test.target.Foo", "returnNullGood()"), -6, APPROVE),
            new TReport(
                new OnMethod("Foo.java", "test.target.Foo", "returnNullBad()"), -5, APPROVE))
        .setPredicate(
            (expected, found) ->
                expected.root.equals(found.root)
                    && expected.getExpectedValue()
                        == found.getOverallEffect(coreTestHelper.getConfig())
                    && Objects.equals(expected.getTag(), found.getTag()))
        .toDepth(5)
        .disableBailOut()
        .enableDownstreamDependencyAnalysis(LOWER_BOUND)
        .start();
  }

  @Test
  public void upperBoundModeTest() {
    coreTestHelper
        .addExpectedReports(
            new TReport(
                new OnMethod("Foo.java", "test.target.Foo", "returnNullGood()"), -6, APPROVE),
            // Resolves 6 errors locally but creates 1 error on downstream dependencies that cannot
            // be resolved, one of the triggered fixes in the tree, triggers an error in downstream
            // dependency, the overall effect is -6 + 1 + 1 = -4 in upper bound mode.
            new TReport(
                new OnMethod("Foo.java", "test.target.Foo", "returnNullBad()"), -4, APPROVE))
        .setPredicate(
            (expected, found) ->
                expected.root.equals(found.root)
                    && expected.getExpectedValue()
                        == found.getOverallEffect(coreTestHelper.getConfig())
                    && Objects.equals(expected.getTag(), found.getTag()))
        .toDepth(5)
        .disableBailOut()
        .enableDownstreamDependencyAnalysis(UPPER_BOUND)
        .start();
  }
}
