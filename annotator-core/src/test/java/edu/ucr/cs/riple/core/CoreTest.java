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
import static java.util.Collections.singleton;

import edu.ucr.cs.riple.core.tools.TReport;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.injector.location.OnMethod;
import edu.ucr.cs.riple.injector.location.OnParameter;
import java.util.Collections;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CoreTest extends AnnotatorBaseCoreTest {

  public CoreTest() {
    super("nullable-multi-modular");
  }

  @Test
  public void field() {
    coreTestHelper
        .onTarget()
        .withSourceLines("Main.java", "package test;", "public class Main {", "Object field;", "}")
        .withExpectedReports(
            new TReport(new OnField("Main.java", "test.Main", singleton("field")), -1))
        .start();
  }

  @Test
  public void method() {
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Main.java",
            "package test;",
            "public class Main {",
            "   Object run() {",
            "     return null;",
            "   }",
            "}")
        .withExpectedReports(new TReport(new OnMethod("Main.java", "test.Main", "run()"), -1))
        .start();
  }

  @Test
  public void param() {
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Main.java",
            "package test;",
            "public class Main {",
            "   Object run(Object o) {",
            "     return o;",
            "   }",
            "   void pass() {",
            "     run(null);",
            "   }",
            "}")
        .withExpectedReports(
            new TReport(new OnParameter("Main.java", "test.Main", "run(java.lang.Object)", 0), 0))
        .start();
  }

  @Test
  public void paramComplete() {
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Main.java",
            "package test;",
            "public class Main {",
            "   Object run(Object o) {",
            "     return o;",
            "   }",
            "   void pass() {",
            "     run(null);",
            "   }",
            "}")
        .withExpectedReports(
            new TReport(new OnParameter("Main.java", "test.Main", "run(java.lang.Object)", 0), 0),
            new TReport(new OnMethod("Main.java", "test.Main", "run(java.lang.Object)"), -1))
        .activateOuterLoop()
        .start();
  }

  @Test
  public void fieldAssignNullable() {
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Main.java",
            "package test;",
            "public class Main {",
            "   Object field;",
            "   Main() {",
            "     field = null;",
            "   }",
            "   Object getF(){ return field; }",
            "}")
        .withExpectedReports(
            new TReport(new OnField("Main.java", "test.Main", singleton("field")), -1))
        .toDepth(1)
        .start();
  }

  @Test
  public void fieldAssignNullableConstructor() {
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Main.java",
            "package test;",
            "public class Main {",
            "   Object f;",
            "   Main(Object f) {",
            "     this.f = f;",
            "   }",
            "}",
            "class C {",
            "   Main main = new Main(null);",
            "}")
        .withExpectedReports(
            new TReport(new OnParameter("Main.java", "test.Main", "Main(java.lang.Object)", 0), 1))
        .toDepth(1)
        .start();
  }

  @Test
  public void fieldAssignNullableConstructorForceResolveEnabled() {
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Main.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Main {",
            "   Object f;",
            "   Main(Object f) {",
            "      this.f = f;",
            "   }",
            "   Main(Object f, @Nullable Object o) {",
            "      this.f = f;",
            "      Integer h = o.hashCode();",
            "   }",
            "}",
            "class C {",
            "   Main main = new Main(null);",
            "}")
        .withExpectedReports(
            new TReport(new OnParameter("Main.java", "test.Main", "Main(java.lang.Object)", 0), 1))
        .toDepth(1)
        .suppressRemainingErrors()
        .checkExpectedOutput("fieldAssignNullableConstructorForceResolveEnabled/expected")
        .start();
  }

  @Test
  public void multipleFieldDeclarationTest() {
    coreTestHelper
        .onTarget()
        .withSourceDirectory("test", "multiplefielddeclration")
        .withExpectedReports(
            new TReport(
                new OnField("Main.java", "test.Main", newHashSet("f1", "f2", "f3", "f4")), 9),
            new TReport(new OnField("Main.java", "test.Main", singleton("f5")), 1))
        .disableBailOut()
        .toDepth(1)
        .start();
  }

  @Test
  public void multipleFieldDeclarationTestForceResolveEnabled() {
    coreTestHelper
        .onTarget()
        .withSourceDirectory("test", "multiplefielddeclration")
        .withExpectedReports(
            new TReport(
                new OnField("Main.java", "test.Main", newHashSet("f1", "f2", "f3", "f4")), 9),
            new TReport(new OnField("Main.java", "test.Main", singleton("f5")), 1))
        .disableBailOut()
        .toDepth(1)
        // This causes the test to report a failure if any errors remain when building the target.
        .suppressRemainingErrors()
        .start();
  }

  @Test
  public void innerClass() {
    coreTestHelper
        .onTarget()
        .withSourceDirectory("test", "innerclass")
        .withExpectedReports(
            new TReport(new OnMethod("Main.java", "test.Main$1", "bar(java.lang.Object)"), 0),
            new TReport(new OnMethod("Main.java", "test.Main$1Child", "exec()"), 0),
            new TReport(new OnMethod("Main.java", "test.Main$1Child$1Bar", "returnsNull()"), -1),
            new TReport(new OnField("Main.java", "test.Main$1", singleton("f")), -1))
        .start();
  }

  @Test
  public void multipleReturnNullable() {
    coreTestHelper
        .toDepth(4)
        .onTarget()
        .withSourceDirectory("test", "multiplereturnnullable")
        .withExpectedReports(
            new TReport(
                new OnParameter("A.java", "test.A", "helper(java.lang.Object)", 0),
                -5,
                newHashSet(
                    new OnParameter("A.java", "test.A", "foo(java.lang.Object)", 0),
                    new OnMethod("A.java", "test.A", "foo(java.lang.Object"),
                    new OnField("A.java", "test.A", singleton("field"))),
                null),
            new TReport(
                new OnParameter("B.java", "test.B", "run(java.lang.Object)", 0),
                -5,
                newHashSet(
                    new OnMethod("B.java", "test.B", "run(java.lang.Object)"),
                    new OnField("B.java", "test.B", singleton("field"))),
                null))
        .disableBailOut()
        .start();
  }

  @Test
  public void multipleReturnNullableRecursive() {
    coreTestHelper
        .onTarget()
        .withSourceDirectory("test", "multiplereturnnullablerecursive")
        .withExpectedReports(
            new TReport(new OnField("Main.java", "test.Main", singleton("field")), -6),
            new TReport(new OnMethod("Main.java", "test.Main", "returnNullableRecursive()"), -6),
            new TReport(new OnMethod("Main.java", "test.Main", "returnNullable()"), -6))
        .start();
  }

  @Test
  public void overrideMethodDeclaredOutsideModuleTest() {
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Main.java",
            "package test;",
            "import java.util.function.Function;",
            "import java.util.stream.Stream;",
            "public class Main {",
            "   public void run() {",
            "     Stream.of(\"Foo\").map(new Function<Object, String>() {",
            "       @Override",
            "       public String apply(Object o) {",
            "         return null;",
            "       }",
            "     });",
            "   }",
            "}")
        .withExpectedReports(
            new TReport(new OnMethod("Main.java", "test.Main$1", "apply(java.lang.Object)"), -1))
        .toDepth(1)
        .start();
  }

  @Test
  public void deactivateInferenceTest() {
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Main.java",
            "package test;",
            "public class Main {",
            "   public Object run() {",
            "     return null;",
            "   }",
            "}")
        .expectNoReport()
        .deactivateInference()
        .checkExpectedOutput("deactivateInferenceTest/expected")
        .start();
  }

  @Test
  public void errorInFieldDeclarationForceResolveTest() {
    coreTestHelper
        .onTarget()
        .withSourceDirectory("test", "errorInFieldDeclarationForceResolveTest/input")
        .withExpectedReports(
            new TReport(new OnField("Foo.java", "test.Foo", Set.of("f1")), 2),
            new TReport(new OnField("Foo.java", "test.Foo", Set.of("f2", "f3")), 2),
            new TReport(new OnField("Foo.java", "test.Foo", Set.of("f0")), -1),
            new TReport(new OnParameter("Bar.java", "test.Bar", "process(java.lang.Object)", 0), 1),
            new TReport(new OnField("Foo.java", "test.Foo", Set.of("f4")), 1),
            new TReport(new OnField("Foo.java", "test.Foo", Set.of("f5")), 0))
        .toDepth(1)
        .suppressRemainingErrors()
        .checkExpectedOutput("errorInFieldDeclarationForceResolveTest/expected")
        .start();
  }

  @Test
  public void initializationErrorWithMultipleConstructors() {
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Foo.java",
            "package test;",
            "public class Foo {",
            "   Object f1;",
            "   Object f2;",
            "   Object f3;",
            "   Object f4 = null;",
            "   Foo() {}",
            "   Foo(int i) {}",
            "   Foo(int i, int j) {}",
            "   void bar1() {",
            "     f3.hashCode();",
            "     f4.hashCode();",
            "   }",
            "   void bar2() {",
            "     f3.hashCode();",
            "     f4.hashCode();",
            "   }",
            "}")
        .withExpectedReports(
            new TReport(new OnField("Foo.java", "test.Foo", Set.of("f1")), 0),
            new TReport(new OnField("Foo.java", "test.Foo", Set.of("f2")), 0),
            new TReport(new OnField("Foo.java", "test.Foo", Set.of("f3")), 2),
            new TReport(new OnField("Foo.java", "test.Foo", Set.of("f4")), 1))
        .toDepth(1)
        .suppressRemainingErrors()
        .checkExpectedOutput("initializationErrorWithMultipleConstructors/expected")
        .start();
  }

  @Test
  public void fieldNoInitialization() {
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "A.java",
            "package test;",
            "import java.util.Objects;",
            "public class A {",
            "   Object f;",
            "   A() { }",
            "   void run() {",
            "       this.f = foo();",
            "   }",
            "   Object foo() {",
            "        return null;",
            "   }",
            "}")
        .withExpectedReports(
            new TReport(
                // adding @Nullable on foo() will trigger a fix on making field f @Nullable, so far,
                // effect is -1 + 1 (triggered error on this.f = foo()) = 0.
                new OnMethod("A.java", "test.A", "foo()"),
                -2,
                // adding @Nullable on f will resolve the triggered error by foo() and also resolves
                // the initialization error on A() as well.
                // Therefore, the combined effect is -1 + (-1) = -2. It resolves all existing
                // errors.
                Set.of(new OnField("A.java", "test.A", Collections.singleton("f"))),
                Collections.emptySet()),
            new TReport(
                // Adding @Nullable on f will resolve the initialization error and does not trigger
                // any error, effect is -1.
                new OnField("A.java", "test.A", Collections.singleton("f")), -1))
        .toDepth(5)
        .disableBailOut()
        .start();
  }

  @Test
  public void staticAndInstanceInitializerBlockTest() {
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "A.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "import java.util.Objects;",
            "public class A {",
            "   private static Object f;",
            "   static {",
            "      Object a = B.foo();",
            "      Object b = B.bar();",
            "      if (Objects.hashCode(a) > Objects.hashCode(b)) {",
            "         f = a;",
            "      } else {",
            "         f = b;",
            "      }",
            "   }",
            "}",
            "class B {",
            "   {",
            "      foo().hashCode();",
            "   }",
            "   @Nullable",
            "   public static Object foo() { return null; }",
            "   @Nullable",
            "   public static Object bar() { return null; }",
            "}")
        .withExpectedReports(new TReport(new OnField("A.java", "test.A", singleton("f")), -3))
        .toDepth(5)
        .suppressRemainingErrors()
        .checkExpectedOutput("staticAndInstanceInitializerBlockTest/expected")
        .start();
  }

  @Test
  public void acknowledgeNonnullAnnotations() {
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "A.java",
            "package test;",
            "import javax.annotation.Nonnull;",
            "public class A {",
            "   @Nonnull private Object field;",
            "   @Nonnull Object foo(@Nonnull Object param) {",
            "       return null;",
            "   }",
            "   void bar() {",
            "      foo(null);",
            "   }",
            "}")
        .expectNoReport()
        .toDepth(5)
        .start();
    // No annotation should be added, since they are annotated as @Nonnull although each can reduce
    // the number of errors.
    Assert.assertEquals(coreTestHelper.getLog().getInjectedAnnotations().size(), 0);
  }

  @Test
  public void impactedLambdaAndMemberReferenceParameterNullableTest() {
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Main.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Main {",
            "   public void f(Foo r){ }",
            "   public void baz1(){",
            "     f(new Foo(){",
            "       @Override",
            "       public void bar(Object o){",
            "         take(o);",
            "       }",
            "     });",
            "   }",
            "   public void baz2() {",
            "       f(e -> take(e));",
            "   }",
            "   public void baz3() {",
            "       f(this::take);",
            "   }",
            "   public void take(Object o){ }",
            "   public void passNull(Foo foo) {",
            "       foo.bar(null);",
            "   }",
            "}")
        .withSourceLines(
            "Foo.java", "package test;", "public interface Foo{", "     void bar(Object o);", "}")
        .withExpectedReports(
            new TReport(new OnParameter("Foo.java", "test.Foo", "bar(java.lang.Object)", 0), 2))
        .toDepth(1)
        .start();
  }

  @Test
  public void impactedLambdaAndMemberReferenceReturnNullableTest() {
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Main.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Main {",
            "   public void f(Foo r){ }",
            "   public void baz1() {",
            "       f(e -> bar(e));",
            "   }",
            "   public void baz2() {",
            "       f(this::bar);",
            "   }",
            "   public Object bar(Object o){",
            "       return null;",
            "   }",
            "}")
        .withSourceLines(
            "Foo.java", "package test;", "public interface Foo{", "     Object m(Object o);", "}")
        .withExpectedReports(
            new TReport(new OnMethod("Foo.java", "test.Main", "bar(java.lang.Object)"), 1))
        .toDepth(1)
        .start();
  }

  @Test
  public void nestedParameters() {
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "A.java",
            "package test;",
            "import java.util.Objects;",
            "public class A {",
            "   public void c1() {",
            "      f1(null);",
            "   }",
            "   public void c2() {",
            "      f2(null);",
            "   }",
            "   public void f1(Object p1) {",
            "      f2(p1);",
            "   }",
            "   public void f2(Object p2) {",
            "      ",
            "   }",
            "}")
        .withExpectedReports(
            new TReport(new OnParameter("A.java", "test.A", "f1(java.lang.Object)", 0), 0),
            new TReport(new OnParameter("A.java", "test.A", "f2(java.lang.Object)", 0), -1))
        .toDepth(1)
        .start();
  }
}
