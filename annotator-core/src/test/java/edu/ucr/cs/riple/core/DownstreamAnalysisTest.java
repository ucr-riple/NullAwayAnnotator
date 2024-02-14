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

import static edu.ucr.cs.riple.core.AnalysisMode.STRICT;
import static edu.ucr.cs.riple.core.Report.Tag.APPROVE;
import static edu.ucr.cs.riple.core.Report.Tag.REJECT;

import edu.ucr.cs.riple.core.tools.TReport;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.injector.location.OnMethod;
import java.util.Set;
import org.junit.Test;

public class DownstreamAnalysisTest extends AnnotatorBaseCoreTest {

  public DownstreamAnalysisTest() {
    super("nullable-multi-modular");
  }

  @Test
  public void publicMethodWithDownstreamDependencyEnabled() {
    coreTestHelper
        .onTarget()
        .withSourceFile("Foo.java", "downstreamDependencyMethodCheck/Foo.java")
        .withDependency("DepA")
        .withSourceFile("DepA.java", "downstreamDependencyMethodCheck/DepA.java")
        .withDependency("DepB")
        .withSourceFile("DepB.java", "downstreamDependencyMethodCheck/DepB.java")
        .withDependency("DepC")
        .withSourceFile("DepC.java", "downstreamDependencyMethodCheck/DepC.java")
        .withExpectedReports(
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
        .onTarget()
        .withSourceFile("Foo.java", "downstreamDependencyMethodCheck/Foo.java")
        .withDependency("DepA")
        .withSourceFile("DepA.java", "downstreamDependencyMethodCheck/DepA.java")
        .withDependency("DepB")
        .withSourceFile("DepB.java", "downstreamDependencyMethodCheck/DepB.java")
        .withDependency("DepC")
        .withSourceFile("DepC.java", "downstreamDependencyMethodCheck/DepC.java")
        .withExpectedReports(
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
  public void publicFieldWithDownstreamDependencyEnabled() {
    coreTestHelper
        .onTarget()
        .withSourceFile("Foo.java", "downstreamDependencyFieldCheck/Foo.java")
        .withDependency("DepA")
        .withSourceFile("DepA.java", "downstreamDependencyFieldCheck/DepA.java")
        .withDependency("DepB")
        .withSourceFile("DepB.java", "downstreamDependencyFieldCheck/DepB.java")
        .withDependency("DepC")
        .withSourceFile("DepC.java", "downstreamDependencyFieldCheck/DepC.java")
        .withExpectedReports(
            // Effect on target is -1, Effect on DepA is 0 and on DepB and DepC is 1 ->
            // Lower bound is 2. And overall effect is -1 + 2 = 1. Effect is greater than 0 and
            // triggers unresolved errors on downstream dependencies, hence the tree should be
            // tagged as REJECT.
            new TReport(new OnField("Foo.java", "test.target.Foo", Set.of("f")), 1, REJECT),
            // Effect on target is -2. Root is on f1, but it triggers making f @Nullable as well.
            // Fix tree containing both fixes resolves two errors leaving no remaining error, effect
            // on target is -2. But f creates 2 errors on downstream dependencies, hence the lower
            // bound is 2. Overall effect is -2 + 2 = 0. Since f creates unresolved errors on
            // downstream dependencies, the tree should be tagged as REJECT even though the overall
            // effect is not greater than 0.
            new TReport(new OnField("Foo.java", "test.target.Foo", Set.of("f1")), 0, REJECT),
            // Effect on target is -1. Root is on f2, but it triggers making f3 @Nullable through an
            // assignment in DepA and the tree is extended to include the corresponding fix. Since,
            // f2 creates a resolvable error in downstream dependencies that the corresponding fix
            // is present in the fix tree, the lower bound effect is 0. Overall effect is -1 + 0 =
            // -1. Since the overall effect is less than 0, with no error in downstream
            // dependencies, the tree should be tagged as APPROVE.
            new TReport(new OnField("Foo.java", "test.target.Foo", Set.of("f2")), -1, APPROVE))
        .setPredicate(
            (expected, found) ->
                // check for root equality
                expected.root.equals(found.root)
                    && expected.getOverallEffect(coreTestHelper.getConfig())
                        // check for overall effect equality
                        == found.getOverallEffect(coreTestHelper.getConfig())
                    // check for tag equality
                    && expected.getTag().equals(found.getTag()))
        .toDepth(5)
        .disableBailOut()
        .enableDownstreamDependencyAnalysis(STRICT)
        .start();
  }

  @Test
  public void lowerBoundComputationTest() {
    coreTestHelper
        .onTarget()
        .withSourceFile("Foo.java", "downstreamDependencyMethodCheck/Foo.java")
        .withDependency("DepA")
        .withSourceFile("DepA.java", "downstreamDependencyMethodCheck/DepA.java")
        .withDependency("DepB")
        .withSourceFile("DepB.java", "downstreamDependencyMethodCheck/DepB.java")
        .withDependency("DepC")
        .withSourceFile("DepC.java", "downstreamDependencyMethodCheck/DepC.java")
        .withExpectedReports(
            // Only returnNullableBad triggers new errors in this fix chain (+10), lower bound is
            // 10.
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
        .activateOuterLoop()
        .start();
  }

  @Test
  public void upperBoundComputationTest() {
    coreTestHelper
        .onTarget()
        .withSourceFile("Foo.java", "downstreamDependencyMethodCheck/Foo.java")
        .withDependency("DepA")
        .withSourceFile("DepA.java", "downstreamDependencyMethodCheck/DepA.java")
        .withDependency("DepB")
        .withSourceFile("DepB.java", "downstreamDependencyMethodCheck/DepB.java")
        .withDependency("DepC")
        .withSourceFile("DepC.java", "downstreamDependencyMethodCheck/DepC.java")
        .withExpectedReports(
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
        .activateOuterLoop()
        .start();
  }
}
