/*
 * MIT License
 *
 * Copyright (c) 2023 Nima Karimipour
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

package edu.ucr.cs.riple.injector;

import com.google.common.collect.ImmutableList;
import edu.ucr.cs.riple.injector.changes.AddTypeUseMarkerAnnotation;
import edu.ucr.cs.riple.injector.changes.RemoveTypeUseMarkerAnnotation;
import edu.ucr.cs.riple.injector.location.OnField;
import java.util.Collections;

import edu.ucr.cs.riple.injector.location.OnParameter;
import org.junit.Test;

public class OnArrayInjectionTest extends BaseInjectorTest {

  @Test
  public void t1() {
    injectorTestHelper
        .addInput("Foo.java", "package test;", "public class Foo {", "   Object[] arr;", "}")
        .expectOutput(
            "package test;",
            "import custom.annot.Untainted;",
            "public class Foo {",
            "   Object@Untainted [] arr;",
            "}")
        .addChanges(
            new AddTypeUseMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Collections.singleton("arr")),
                "custom.annot.Untainted",
                ImmutableList.of(ImmutableList.of(0))))
        .start();
  }

  @Test
  public void t2() {
    injectorTestHelper
        .addInput("Foo.java", "package test;", "public class Foo {", "   Object[] arr;", "}")
        .expectOutput(
            "package test;",
            "import custom.annot.Untainted;",
            "public class Foo {",
            "   @Untainted Object[] arr;",
            "}")
        .addChanges(
            new AddTypeUseMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Collections.singleton("arr")),
                "custom.annot.Untainted",
                ImmutableList.of(ImmutableList.of(1, 0))))
        .start();
  }

  @Test
  public void t3() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import custom.annot.Untainted;",
            "public class Foo {",
            "   @Untainted Object[] arr;",
            "}")
        .expectOutput(
            "package test;",
            "import custom.annot.Untainted;",
            "public class Foo {",
            "   Object[] arr;",
            "}")
        .addChanges(
            new RemoveTypeUseMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Collections.singleton("arr")),
                "custom.annot.Untainted",
                ImmutableList.of(ImmutableList.of(1, 0))))
        .start();
  }

  @Test
  public void t4() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import custom.annot.Untainted;",
            "public class Foo {",
            "   Object@Untainted [] arr;",
            "}")
        .expectOutput(
            "package test;",
            "import custom.annot.Untainted;",
            "public class Foo {",
            "   Object[] arr;",
            "}")
        .addChanges(
            new RemoveTypeUseMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Collections.singleton("arr")),
                "custom.annot.Untainted",
                ImmutableList.of(ImmutableList.of(0))))
        .start();
  }

  @Test
  public void t5() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import custom.annot.Untainted;",
            "public class Foo {",
            "   Object arr @Untainted [];",
            "}")
        .expectOutput(
            "package test;",
            "import custom.annot.Untainted;",
            "public class Foo {",
            "   Object arr [];",
            "}")
        .addChanges(
            new RemoveTypeUseMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Collections.singleton("arr")),
                "custom.annot.Untainted",
                ImmutableList.of(ImmutableList.of(0))))
        .start();
  }

  @Test
  public void t6() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import custom.annot.Untainted;",
            "public class Foo {",
            "   void m(Object... arr){};",
            "}")
        .expectOutput(
            "package test;",
            "import custom.annot.Untainted;",
            "public class Foo {",
            "   void m(@Untainted Object... arr){};",
            "}")
        .addChanges(
            new AddTypeUseMarkerAnnotation(
                new OnParameter("Foo.java", "test.Foo", "m(java.lang.Object[])", 0),
                "custom.annot.Untainted",
                ImmutableList.of(ImmutableList.of(0))))
        .start();
  }

  @Test
  public void t7() {
    injectorTestHelper
            .addInput(
                    "Foo.java",
                    "package test;",
                    "import custom.annot.Untainted;",
                    "public class Foo {",
                    "   void m(Object... arr){};",
                    "}")
            .expectOutput(
                    "package test;",
                    "import custom.annot.Untainted;",
                    "public class Foo {",
                    "   void m(Object@Untainted... arr){};",
                    "}")
            .addChanges(
                    new AddTypeUseMarkerAnnotation(
                            new OnParameter("Foo.java", "test.Foo", "m(java.lang.Object[])", 0),
                            "custom.annot.Untainted",
                            ImmutableList.of(ImmutableList.of(1, 0))))
            .start();
  }
}
