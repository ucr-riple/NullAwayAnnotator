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

import edu.ucr.cs.riple.core.tools.TReport;
import edu.ucr.cs.riple.injector.location.OnMethod;
import edu.ucr.cs.riple.injector.location.OnParameter;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class ArgumentNullableFlowTest extends BaseCoreTest {

  public ArgumentNullableFlowTest() {
    super("nullable-flow-test", List.of("Target", "DepA", "DepB", "DepC"));
  }

  @Test
  public void nullableFlowDetectionEnabledTest() {
    coreTestHelper
        .addExpectedReports(
            // Change reduces errors on target by -5, but increases them in downstream dependency
            // DepA by 2, DepB by 0 and DepC by 1. Hence, the total effect is: -2.
            new TReport(new OnMethod("Foo.java", "test.target.Foo", "returnNullable(int)"), -2),
            // Report below should appear due to flow of Nullable back to target by downstream
            // dependencies. The triggered error is resolvable by making the method @Nullable, therefore,
            // the overall effect is 0.
            new TReport(
                new OnParameter(
                    "Foo.java", "test.target.Foo", "bar1(java.lang.Object,java.lang.Object)", 0),
                0,
                newHashSet(
                    new OnMethod(
                        "Foo.java", "test.target.Foo", "bar1(java.lang.Object,java.lang.Object)")),
                Collections.emptySet()),
            // Report below should appear due to flow of Nullable back to target by downstream
            // dependencies. The triggered error is not resolvable, therefore, the overall effect 1.
            new TReport(
                new OnParameter(
                    "Foo.java", "test.target.Foo", "bar2(java.lang.Object,java.lang.Object)", 1),
                1))
        .toDepth(5)
        .requestCompleteLoop()
        .enableDownstreamDependencyAnalysis()
        .start();
  }

  @Test
  public void nullableFlowDetectionDisabledTest() {
    coreTestHelper
        .addExpectedReports(
            // Change reduces errors on target by -5
            new TReport(new OnMethod("Foo.java", "test.target.Foo", "returnNullable(int)"), -5))
        .setPredicate(
            (expected, found) ->
                expected.root.equals(found.root)
                    && expected.getOverallEffect(coreTestHelper.getConfig())
                        == found.getOverallEffect(coreTestHelper.getConfig()))
        .toDepth(5)
        .requestCompleteLoop()
        .start();
  }
}
