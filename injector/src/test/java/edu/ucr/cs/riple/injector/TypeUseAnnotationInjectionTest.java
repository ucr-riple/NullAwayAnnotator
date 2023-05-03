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

import edu.ucr.cs.riple.injector.changes.AddTypeUseMarkerAnnotation;
import edu.ucr.cs.riple.injector.changes.RemoveTypeUseAnnotation;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.injector.location.OnLocalVariable;
import edu.ucr.cs.riple.injector.location.OnMethod;
import edu.ucr.cs.riple.injector.location.OnParameter;
import java.util.Set;
import org.junit.Test;

public class TypeUseAnnotationInjectionTest extends BaseInjectorTest {

  @Test
  public void additionTest() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "public class Foo {",
            "   java.lang.Object bar;",
            "   java.lang.Object baz(java.lang.Object param) {;",
            "       java.lang.Object localVar;",
            "       return new Object();",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import custom.example.Untainted;",
            "public class Foo {",
            "   java.lang.@Untainted Object bar;",
            "   java.lang.@Untainted Object baz(java.lang.@Untainted Object param) {;",
            "       java.lang.@Untainted Object localVar;",
            "       return new Object();",
            "   }",
            "}")
        .addChanges(
            new AddTypeUseMarkerAnnotation(
                new OnField("Foo.java", "test.Foo", Set.of("bar")), "custom.example.Untainted"),
            new AddTypeUseMarkerAnnotation(
                new OnMethod("Foo.java", "test.Foo", "baz(java.lang.Object)"),
                "custom.example.Untainted"),
            new AddTypeUseMarkerAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "baz(java.lang.Object)", "localVar"),
                "custom.example.Untainted"),
            new AddTypeUseMarkerAnnotation(
                new OnParameter("Foo.java", "test.Foo", "baz(java.lang.Object)", 0),
                "custom.example.Untainted"))
        .start();
  }

  @Test
  public void deletionTest() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import custom.example.Untainted;",
            "public class Foo {",
            "   java.lang.@Untainted Object bar;",
            "   java.lang.@Untainted Object baz(java.lang.@Untainted Object param) {;",
            "       java.lang.@Untainted Object localVar;",
            "       return new Object();",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import custom.example.Untainted;",
            "public class Foo {",
            "   java.lang.Object bar;",
            "   java.lang.Object baz(java.lang.Object param) {;",
            "       java.lang.Object localVar;",
            "       return new Object();",
            "   }",
            "}")
        .addChanges(
            new RemoveTypeUseAnnotation(
                new OnField("Foo.java", "test.Foo", Set.of("bar")), "custom.example.Untainted"),
            new RemoveTypeUseAnnotation(
                new OnMethod("Foo.java", "test.Foo", "baz(java.lang.Object)"),
                "custom.example.Untainted"),
            new RemoveTypeUseAnnotation(
                new OnLocalVariable("Foo.java", "test.Foo", "baz(java.lang.Object)", "localVar"),
                "custom.example.Untainted"),
            new RemoveTypeUseAnnotation(
                new OnParameter("Foo.java", "test.Foo", "baz(java.lang.Object)", 0),
                "custom.example.Untainted"))
        .start();
  }
}
