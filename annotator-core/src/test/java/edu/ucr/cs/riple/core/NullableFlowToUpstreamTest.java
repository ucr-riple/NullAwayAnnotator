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
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.injector.location.OnMethod;
import edu.ucr.cs.riple.injector.location.OnParameter;
import java.util.Collections;
import java.util.Set;
import org.junit.Test;

public class NullableFlowToUpstreamTest extends AnnotatorBaseCoreTest {

  public NullableFlowToUpstreamTest() {
    super("nullable-multi-modular");
  }

  @Test
  public void nullableFlowDetectionEnabledTest() {
    coreTestHelper
        .onTarget()
        .withSourceFile("Foo.java", "nullableflow/Foo.java")
        .withDependency("DepA")
        .withSourceFile("DepA.java", "nullableflow/DepA.java")
        .withDependency("DepB")
        .withSourceFile("DepB.java", "nullableflow/DepB.java")
        .withDependency("DepC")
        .withSourceFile("DepC.java", "nullableflow/DepC.java")
        .withExpectedReports(
            // Change reduces errors on target by -5, but increases them in downstream dependency
            // DepA by 1 (Resolvable), DepB by 0 and DepC by 2 (1 resolvable).
            // Parameters param1 and param2 in bar1 and bar2 will receive Nullable from downstream
            // dependencies,
            // param1 will be @Nullable with no triggered errors, param2 will trigger one error.
            // Hence, the overall effect is: -5 + (from downstream dependency) 1 + (in target) 1 =
            // -3.
            new TReport(
                new OnMethod("Foo.java", "test.target.Foo", "returnNullable(int)"),
                -3,
                newHashSet(
                    new OnParameter(
                        "Foo.java",
                        "test.target.Foo",
                        "bar1(java.lang.Object,java.lang.Object)",
                        0),
                    new OnParameter(
                        "Foo.java",
                        "test.target.Foo",
                        "bar2(java.lang.Object,java.lang.Object)",
                        1),
                    new OnMethod(
                        "Foo.java", "test.target.Foo", "bar1(java.lang.Object,java.lang.Object)")),
                Collections.emptySet()),
            // Change creates two errors on downstream dependencies (1 resolvable) and resolves one
            // error locally, therefore the overall effect is 0.
            new TReport(
                new OnMethod("Foo.java", "test.target.Foo", "getNull()"),
                0,
                newHashSet(
                    new OnParameter(
                        "Foo.java", "test.target.Foo", "takeNull(java.lang.Object)", 0)),
                Collections.emptySet()))
        .setPredicate((expected, found) -> expected.testEquals(coreTestHelper.getConfig(), found))
        .toDepth(5)
        .disableBailOut()
        .enableDownstreamDependencyAnalysis()
        .start();
  }

  @Test
  public void nullableFlowDetectionDisabledTest() {
    coreTestHelper
        .onTarget()
        .withSourceFile("Foo.java", "nullableflow/Foo.java")
        .withDependency("DepA")
        .withSourceFile("DepA.java", "nullableflow/DepA.java")
        .withDependency("DepB")
        .withSourceFile("DepB.java", "nullableflow/DepB.java")
        .withDependency("DepC")
        .withSourceFile("DepC.java", "nullableflow/DepC.java")
        .withExpectedReports(
            new TReport(new OnMethod("Foo.java", "test.target.Foo", "returnNullable(int)"), -5),
            new TReport(new OnMethod("Foo.java", "test.target.Foo", "getNull()"), -1))
        .setPredicate((expected, found) -> expected.testEquals(coreTestHelper.getConfig(), found))
        .toDepth(5)
        .disableBailOut()
        .start();
  }

  @Test
  public void upperBoundCountForResolvableErrorOnDownstreamTest() {
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Bar.java",
            "package test;",
            "public class Bar {",
            "   public String foo;",
            "   public String foo2 = \"\";",
            "   public String getFoo() {",
            "     return foo;",
            "   }",
            "}")
        .withDependency("Dep")
        .withSourceLines(
            "Dep.java",
            "package test.dep;",
            "import test.Bar;",
            "public class Dep {",
            "   public Bar bar = new Bar();",
            "   public void exec() {",
            "     bar.foo2 = bar.getFoo();",
            "   }",
            "}")
        .withExpectedReports(
            new TReport(
                new OnField("Bar.java", "test.Bar", Set.of("foo")),
                -1,
                // fixes in tree:
                Set.of(
                    new OnMethod("Bar.java", "test.Bar", "getFoo(String)"),
                    // coming from flow of nullable back to target through a field write.
                    new OnField("Bar.java", "test.Bar", Set.of("foo2"))),
                Set.of()))
        .disableBailOut()
        .enableDownstreamDependencyAnalysis(AnalysisMode.STRICT)
        .toDepth(5)
        .start();
  }
}
