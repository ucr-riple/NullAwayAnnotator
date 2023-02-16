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

import edu.ucr.cs.riple.core.tools.TReport;
import edu.ucr.cs.riple.injector.location.OnMethod;
import java.util.List;
import org.junit.Test;

public class DownstreamAnalysisTest extends BaseCoreTest {

  public DownstreamAnalysisTest() {
    super("downstream-dependency-test", List.of("Target", "DepA", "DepB", "DepC"));
  }

  @Test
  public void publicMethodWithDownstreamDependencyEnabled() {
    coreTestHelper
        .addExpectedReports(
            // Change reduces errors on target by -4, but increases them in downstream dependency
            // DepA by 3, DepB by 4 and DepC by 3. Hence, the total effect is: 6.
            new TReport(new OnMethod("Foo.java", "test.target.Foo", "returnNullableBad(int)"), 6),
            // Change reduces errors on target by -5, but increases them in downstream dependency
            // DepA by 0, DepB by 1 and DepC by 0. Hence, the total effect is: -4.
            new TReport(new OnMethod("Foo.java", "test.target.Foo", "returnNullableGood(int)"), -4),
            // Change increases errors on target by 1, and also increases them in downstream
            // dependency DepA by 1. Hence , the total effect is: 2.
            new TReport(new OnMethod("Foo.java", "test.target.Foo", "bar()"), 2))
        .setPredicate(
            (expected, found) ->
                expected.root.equals(found.root)
                    && expected.getOverallEffect(coreTestHelper.getConfig())
                        == found.getOverallEffect(coreTestHelper.getConfig()))
        .toDepth(1)
        .enableDownstreamDependencyAnalysis()
        .start();
  }

  @Test
  public void publicMethodWithDownstreamDependencyDisabled() {
    coreTestHelper
        .addExpectedReports(
            new TReport(new OnMethod("Foo.java", "test.target.Foo", "returnNullableBad(int)"), -4),
            new TReport(new OnMethod("Foo.java", "test.target.Foo", "returnNullableGood(int)"), -5),
            new TReport(new OnMethod("Foo.java", "test.target.Foo", "bar()"), 1))
        .setPredicate(
            (expected, found) ->
                expected.root.equals(found.root)
                    && expected.getOverallEffect(coreTestHelper.getConfig())
                        == found.getOverallEffect(coreTestHelper.getConfig()))
        .toDepth(1)
        .start();
  }

  @Test
  public void lowerBoundComputationTest() {
    coreTestHelper
        .addExpectedReports(
            // Only returnNullableBad triggers new errors in this fix chain (+9), lower bound is 10.
            new TReport(new OnMethod("Foo.java", "test.target.Foo", "returnNullableBad(int)"), 10),
            // Only returnNullableGood triggers new errors in this fix chain (+1), lower bound is 1.
            new TReport(new OnMethod("Foo.java", "test.target.Foo", "returnNullableGood(int)"), 1),
            // Root fix triggers 1 error on downstream dependency but returnNullableBad is
            // present in the fix tree, therefore the lower bound effect for the tree should be 10.
            new TReport(new OnMethod("Foo.java", "test.target.Foo", "bar()"), 10))
        .setPredicate(
            (expected, found) ->
                expected.root.equals(found.root)
                    && expected.getExpectedValue()
                        == found.getLowerBoundEffectOnDownstreamDependencies())
        .toDepth(5)
        .enableDownstreamDependencyAnalysis()
        .requestCompleteLoop()
        .start();
  }

  @Test
  public void upperBoundComputationTest() {
    coreTestHelper
        .addExpectedReports(
            // Only returnNullableBad triggers new errors in this fix chain (+10) and upper bound
            // should be 10
            new TReport(new OnMethod("Foo.java", "test.target.Foo", "returnNullableBad(int)"), 10),
            // Only returnNullableGood triggers new errors in this fix chain (+1) and upper bound
            // should be 1
            new TReport(new OnMethod("Foo.java", "test.target.Foo", "returnNullableGood(int)"), 1),
            // Root fix triggers 1 error on downstream dependency and returnNullableBad is
            // present in the fix tree and triggers 10 errors on downstream dependency, therefore
            // the upper bound effect for the tree should be 11.
            new TReport(new OnMethod("Foo.java", "test.target.Foo", "bar()"), 11))
        .setPredicate(
            (expected, found) ->
                expected.root.equals(found.root)
                    && expected.getExpectedValue()
                        == found.getUpperBoundEffectOnDownstreamDependencies())
        .toDepth(5)
        .enableDownstreamDependencyAnalysis()
        .requestCompleteLoop()
        .start();
  }
}
