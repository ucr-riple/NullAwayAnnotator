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

import com.google.common.base.Preconditions;
import edu.ucr.cs.riple.core.tools.TReport;
import edu.ucr.cs.riple.injector.changes.AddAnnotation;
import edu.ucr.cs.riple.injector.changes.AddMarkerAnnotation;
import edu.ucr.cs.riple.injector.changes.AddSingleElementAnnotation;
import edu.ucr.cs.riple.injector.location.OnClass;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.injector.location.OnMethod;
import edu.ucr.cs.riple.injector.location.OnParameter;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CoreTest extends BaseCoreTest {

  public CoreTest() {
    super("unittest", List.of("unittest"));
  }

  @Test
  public void field() {
    coreTestHelper
        .addInputLines("Main.java", "package test;", "public class Main {", "Object field;", "}")
        .addExpectedReports(
            new TReport(new OnField("Main.java", "test.Main", singleton("field")), -1))
        .start();
  }

  @Test
  public void method() {
    coreTestHelper
        .addInputLines(
            "Main.java",
            "package test;",
            "public class Main {",
            "   Object run() {",
            "     return null;",
            "   }",
            "}")
        .addExpectedReports(new TReport(new OnMethod("Main.java", "test.Main", "run()"), -1))
        .start();
  }

  @Test
  public void param() {
    coreTestHelper
        .addInputLines(
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
        .addExpectedReports(
            new TReport(new OnParameter("Main.java", "test.Main", "run(java.lang.Object)", 0), 0))
        .start();
  }

  @Test
  public void param_complete() {
    coreTestHelper
        .addInputLines(
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
        .requestCompleteLoop()
        .addExpectedReports(
            new TReport(new OnParameter("Main.java", "test.Main", "run(java.lang.Object)", 0), 0),
            new TReport(new OnMethod("Main.java", "test.Main", "run(java.lang.Object)"), -1))
        .start();
  }

  @Test
  public void field_assign_nullable() {
    coreTestHelper
        .addInputLines(
            "Main.java",
            "package test;",
            "public class Main {",
            "   Object field;",
            "   Main() {",
            "     field = null;",
            "   }",
            "   Object getF(){ return field; }",
            "}")
        .toDepth(1)
        .addExpectedReports(
            new TReport(new OnField("Main.java", "test.Main", singleton("field")), -1))
        .start();
  }

  @Test
  public void fieldAssignNullableConstructor() {
    coreTestHelper
        .addInputLines(
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
        .toDepth(1)
        .addExpectedReports(
            new TReport(new OnParameter("Main.java", "test.Main", "Main(java.lang.Object)", 0), 1))
        .start();
  }

  @Test
  public void fieldAssignNullableConstructorForceResolveEnabled() {
    coreTestHelper
        .addInputLines(
            "Main.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Main {",
            "   Object f;",
            "   Main(Object f) {",
            "     this.f = f;",
            "   }",
            "   Main(Object f, @Nullable Object o) {",
            "     this.f = f;",
            "     Integer h = o.hashCode();",
            "   }",
            "}",
            "class C {",
            "   Main main = new Main(null);",
            "}")
        .toDepth(1)
        .addExpectedReports(
            new TReport(new OnParameter("Main.java", "test.Main", "Main(java.lang.Object)", 0), 1))
        .enableForceResolve()
        .start();
    Set<AddAnnotation> expectedAnnotations =
        Set.of(
            new AddMarkerAnnotation(
                new OnMethod("Main.java", "test.Main", "Main(java.lang.Object)"),
                "org.jspecify.nullness.NullUnmarked"),
            new AddMarkerAnnotation(
                new OnMethod("Main.java", "test.Main", "Main(java.lang.Object,java.lang.Object)"),
                "org.jspecify.nullness.NullUnmarked"));
    Assert.assertEquals(
        expectedAnnotations, Set.copyOf(coreTestHelper.getConfig().log.getInjectedAnnotations()));
  }

  @Test
  public void multipleFieldDeclarationTest() {
    coreTestHelper
        .addInputDirectory("test", "multiplefielddeclration")
        .disableBailOut()
        .addExpectedReports(
            new TReport(
                new OnField("Main.java", "test.Main", newHashSet("f1", "f2", "f3", "f4")), 9),
            new TReport(new OnField("Main.java", "test.Main", singleton("f5")), 1))
        .toDepth(1)
        .start();
  }

  @Test
  public void multipleFieldDeclarationTestForceResolveEnabled() {
    coreTestHelper
        .addInputDirectory("test", "multiplefielddeclration")
        .disableBailOut()
        .addExpectedReports(
            new TReport(
                new OnField("Main.java", "test.Main", newHashSet("f1", "f2", "f3", "f4")), 9),
            new TReport(new OnField("Main.java", "test.Main", singleton("f5")), 1))
        .toDepth(1)
        // This causes the test to report a failure if any errors remain when building the target.
        .enableForceResolve()
        .start();
  }

  @Test
  public void inner_class() {
    coreTestHelper
        .addInputDirectory("test", "innerclass")
        .addExpectedReports(
            new TReport(new OnMethod("Main.java", "test.Main$1", "bar(java.lang.Object)"), 0),
            new TReport(new OnMethod("Main.java", "test.Main$1Child", "exec()"), 0),
            new TReport(new OnMethod("Main.java", "test.Main$1Child$1Bar", "returnsNull()"), -1),
            new TReport(new OnField("Main.java", "test.Main$1", singleton("f")), -1))
        .start();
  }

  @Test
  public void multiple_return_nullable() {
    coreTestHelper
        .toDepth(4)
        .addInputDirectory("test", "multiplereturnnullable")
        .disableBailOut()
        .addExpectedReports(
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
        .start();
  }

  @Test
  public void multiple_return_nullable_recursive() {
    coreTestHelper
        .addInputDirectory("test", "multiplereturnnullablerecursive")
        .addExpectedReports(
            new TReport(new OnField("Main.java", "test.Main", singleton("field")), -6),
            new TReport(new OnMethod("Main.java", "test.Main", "returnNullableRecursive()"), -6),
            new TReport(new OnMethod("Main.java", "test.Main", "returnNullable()"), -6))
        .start();
  }

  @Test
  public void overrideMethodDeclaredOutsideModuleTest() {
    coreTestHelper
        .addInputLines(
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
        .toDepth(1)
        .addExpectedReports(
            new TReport(new OnMethod("Main.java", "test.Main$1", "apply(java.lang.Object)"), -1))
        .start();
  }

  @Test
  public void deactivateInferenceTest() {
    coreTestHelper
        .addInputLines(
            "Main.java",
            "package test;",
            "public class Main {",
            "   public Object run() {",
            "     return null;",
            "   }",
            "}")
        .toDepth(1)
        .deactivateInference()
        .start();
    // Verify that only one @NullUnmarked annotation is injected (No @Nullable injection).
    Preconditions.checkArgument(
        coreTestHelper.getConfig().log.getInjectedAnnotations().size() == 1);
    Preconditions.checkArgument(
        coreTestHelper
            .getConfig()
            .log
            .getInjectedAnnotations()
            .get(0)
            .annotation
            .equals("org.jspecify.nullness.NullUnmarked"));
  }

  @Test
  public void errorInFieldDeclarationForceResolveTest() {
    coreTestHelper
        .addInputDirectory("test", "fielderrorregion")
        .addExpectedReports(
            new TReport(new OnField("Foo.java", "test.Foo", Set.of("f1")), 2),
            new TReport(new OnField("Foo.java", "test.Foo", Set.of("f2", "f3")), 2),
            new TReport(new OnField("Foo.java", "test.Foo", Set.of("f0")), -1),
            new TReport(new OnParameter("Bar.java", "test.Bar", "process(java.lang.Object)", 0), 1),
            new TReport(new OnField("Foo.java", "test.Foo", Set.of("f4")), 1),
            new TReport(new OnField("Foo.java", "test.Foo", Set.of("f5")), 0))
        .toDepth(1)
        .enableForceResolve()
        .start();
    Path srcRoot = coreTestHelper.getSourceRoot();
    Set<AddAnnotation> expectedAnnotations =
        Set.of(
            new AddMarkerAnnotation(
                new OnField(srcRoot.resolve("Foo.java").toString(), "test.Foo", Set.of("f0")),
                "javax.annotation.Nullable"),
            new AddMarkerAnnotation(
                new OnMethod(
                    srcRoot.resolve("Bar.java").toString(),
                    "test.Bar",
                    "process(java.lang.Object)"),
                "org.jspecify.nullness.NullUnmarked"),
            new AddSingleElementAnnotation(
                new OnField(srcRoot.resolve("Foo.java").toString(), "test.Foo", Set.of("f4")),
                "SuppressWarnings",
                "NullAway",
                false),
            new AddSingleElementAnnotation(
                new OnField(srcRoot.resolve("Foo.java").toString(), "test.Foo", Set.of("f2", "f3")),
                "SuppressWarnings",
                "NullAway.Init",
                false),
            new AddSingleElementAnnotation(
                new OnField(srcRoot.resolve("Foo.java").toString(), "test.Foo", Set.of("f1")),
                "SuppressWarnings",
                "NullAway.Init",
                false),
            new AddMarkerAnnotation(
                new OnField(srcRoot.resolve("Foo.java").toString(), "test.Foo", Set.of("f5")),
                "javax.annotation.Nullable"));
    // todo: change test infrastructure to do the expected added annotations comparison internally
    // in the upcoming refactoring cycle.
    Assert.assertEquals(
        expectedAnnotations, Set.copyOf(coreTestHelper.getConfig().log.getInjectedAnnotations()));
  }

  @Test
  public void initializationErrorWithMultipleConstructors() {
    coreTestHelper
        .addInputLines(
            "Foo.java",
            "package test;",
            "public class Foo {",
            "   Object f1;",
            "   Object f2;",
            "   Object f3;",
            "   Object f4 = null;",
            "   Foo() { }",
            "   Foo(int i) { }",
            "   Foo(int i, int j) { }",
            "   void bar1() {",
            "     f3.hashCode();",
            "     f4.hashCode();",
            "   }",
            "   void bar2() {",
            "     f3.hashCode();",
            "     f4.hashCode();",
            "   }",
            "}")
        .addExpectedReports(
            new TReport(new OnField("Foo.java", "test.Foo", Set.of("f1")), 0),
            new TReport(new OnField("Foo.java", "test.Foo", Set.of("f2")), 0),
            new TReport(new OnField("Foo.java", "test.Foo", Set.of("f3")), 2),
            new TReport(new OnField("Foo.java", "test.Foo", Set.of("f4")), 1))
        .toDepth(1)
        .enableForceResolve()
        .start();
    String pathToFoo = coreTestHelper.getSourceRoot().resolve("Foo.java").toString();
    Set<AddAnnotation> expectedAnnotations =
        Set.of(
            new AddMarkerAnnotation(
                new OnField(pathToFoo, "test.Foo", Set.of("f1")), "javax.annotation.Nullable"),
            new AddMarkerAnnotation(
                new OnField(pathToFoo, "test.Foo", Set.of("f2")), "javax.annotation.Nullable"),
            new AddSingleElementAnnotation(
                new OnField(pathToFoo, "test.Foo", Set.of("f3")),
                "SuppressWarnings",
                "NullAway.Init",
                false),
            new AddSingleElementAnnotation(
                new OnField(pathToFoo, "test.Foo", Set.of("f4")),
                "SuppressWarnings",
                "NullAway",
                false));
    Assert.assertEquals(
        expectedAnnotations, Set.copyOf(coreTestHelper.getConfig().log.getInjectedAnnotations()));
  }

  @Test
  public void fieldNoInitialization() {
    coreTestHelper
        .addInputLines(
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
        .toDepth(5)
        .disableBailOut()
        .addExpectedReports(
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
        .start();
  }

  @Test
  public void staticAndInstanceInitializerBlockTest() {
    coreTestHelper
        .addInputLines(
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
            "          f = a;",
            "      } else {",
            "          f = b;",
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
        .toDepth(5)
        .addExpectedReports(new TReport(new OnField("A.java", "test.A", singleton("f")), -3))
        .enableForceResolve()
        .start();
    List<AddAnnotation> expectedAnnotations =
        List.of(
            new AddMarkerAnnotation(
                new OnField(
                    coreTestHelper.getSourceRoot().resolve("A.java").toString(),
                    "test.A",
                    Set.of("f")),
                "javax.annotation.Nullable"),
            new AddMarkerAnnotation(
                new OnClass(coreTestHelper.getSourceRoot().resolve("A.java").toString(), "test.B"),
                "org.jspecify.nullness.NullUnmarked"));
    Assert.assertEquals(
        expectedAnnotations, coreTestHelper.getConfig().log.getInjectedAnnotations());
  }

  @Test
  public void acknowledgeNonnullAnnotations() {
    coreTestHelper
        .addInputLines(
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
        .toDepth(5)
        .start();
    // No annotation should be added, since they are annotated as @Nonnull although each can reduce
    // the number of errors.
    Assert.assertEquals(coreTestHelper.getConfig().log.getInjectedAnnotations().size(), 0);
  }

  @Test
  public void impactedLambdaAndMemberReferenceParameterNullableTest() {
    coreTestHelper
        .addInputLines(
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
        .addInputLines(
            "Foo.java", "package test;", "public interface Foo{", "     void bar(Object o);", "}")
        .addExpectedReports(
            new TReport(new OnParameter("Foo.java", "test.Foo", "bar(java.lang.Object)", 0), 2))
        .toDepth(1)
        .start();
  }

  @Test
  public void impactedLambdaAndMemberReferenceReturnNullableTest() {
    coreTestHelper
        .addInputLines(
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
        .addInputLines(
            "Foo.java", "package test;", "public interface Foo{", "     Object m(Object o);", "}")
        .addExpectedReports(
            new TReport(new OnMethod("Foo.java", "test.Main", "bar(java.lang.Object)"), 1))
        .toDepth(1)
        .start();
  }
}
