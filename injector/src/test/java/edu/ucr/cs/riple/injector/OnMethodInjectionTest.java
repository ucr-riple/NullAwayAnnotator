/*
 * MIT License
 *
 * Copyright (c) 2020 Nima Karimipour
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

import edu.ucr.cs.riple.injector.changes.AddMarkerAnnotation;
import edu.ucr.cs.riple.injector.location.OnMethod;
import edu.ucr.cs.riple.injector.location.OnParameter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class OnMethodInjectionTest extends BaseInjectorTest {

  @Test
  public void methodNullableSimple() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "public class Foo {",
            "   Object test(boolean flag) {",
            "       return new Object();",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   @Nullable Object test(boolean flag) {",
            "       return new Object();",
            "   }",
            "}")
        .addInput(
            "test/Foob.java",
            "package test.test;",
            "public class Foob extends Foo {",
            "   Object test(boolean flag) {",
            "       return new Object();",
            "   }",
            "}")
        .expectOutput(
            "package test.test;",
            "import javax.annotation.Nullable;",
            "public class Foob extends Foo {",
            "   @Nullable Object test(boolean flag) {",
            "       return new Object();",
            "   }",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnMethod("Foo.java", "test.Foo", "test(boolean)"), "javax.annotation.Nullable"),
            new AddMarkerAnnotation(
                new OnMethod("test/Foob.java", "test.test.Foob", "test(boolean)"),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void methodNullableEnumSimple() {
    injectorTestHelper
        .addInput(
            "Main.java",
            "package test;",
            "public class Main {",
            "   public enum Test{",
            "     CLASSIC;",
            "     public Object run(){",
            "       return null;",
            "     }",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Main {",
            "   public enum Test{",
            "     CLASSIC;",
            "     @Nullable public Object run(){",
            "       return null;",
            "     }",
            "   }",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnMethod("Main.java", "test.Main$Test", "run()"), "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void methodNullableInnerClass() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   @Nullable",
            "   Object test(boolean flag) {",
            "       return new Object();",
            "   }",
            "   class FooInner {",
            "       Object bar(@Nullable Object foo) {",
            "           return foo;",
            "       }",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   @Nullable",
            "   Object test(boolean flag) {",
            "       return new Object();",
            "   }",
            "   class FooInner {",
            "       @Nullable Object bar(@Nullable Object foo) {",
            "           return foo;",
            "       }",
            "   }",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnMethod("Foo.java", "test.Foo$FooInner", "bar(java.lang.Object)"),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void methodNullableClassDeclInMethod() {
    injectorTestHelper
        .addInput(
            "Main.java",
            "package test;",
            "public class Main {",
            "   void run() {",
            "       class Helper {",
            "         public Object run() { return null; }",
            "       }",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Main {",
            "   void run() {",
            "       class Helper {",
            "         @Nullable public Object run() { return null; }",
            "       }",
            "   }",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnMethod("Main.java", "test.Main$1Helper", "run()"),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void methodNullableSignatureDuplicateType() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "public class Foo {",
            "   Object test(Object flag, String name, String lastname) {",
            "       if(flag == null) {",
            "           return new Object();",
            "       }",
            "       else return new Object();",
            "   }",
            "   Object test(Object flag, Object name, String lastname) {",
            "       if(flag == null) {",
            "           return new Object();",
            "       }",
            "       else return new Object();",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   @Nullable Object test(Object flag, String name, String lastname) {",
            "       if(flag == null) {",
            "           return new Object();",
            "       }",
            "       else return new Object();",
            "   }",
            "   Object test(Object flag, @Nullable Object name, String lastname) {",
            "       if(flag == null) {",
            "           return new Object();",
            "       }",
            "       else return new Object();",
            "   }",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnMethod("Foo.java", "test.Foo", "test(Object, String, String)"),
                "javax.annotation.Nullable"),
            new AddMarkerAnnotation(
                new OnParameter("Foo.java", "test.Foo", "test(Object, Object, String)", 1),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void methodNullableSingleGenericMethodPick() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "public class Foo {",
            "   public IntSet getPredNodeNumbers(T node) throws UnimplementedError {",
            "       Assertions.UNREACHABLE();",
            "       return null;",
            "   }",
            "  OrdinalSet<Statement> computeResult(",
            "        Statement s,",
            "        Map<PointerKey, MutableIntSet> pointerKeyMod,",
            "        BitVectorSolver<? extends ISSABasicBlock> solver,",
            "        OrdinalSetMapping<Statement> domain,",
            "        CGNode node,",
            "        ExtendedHeapModel h,",
            "        PointerAnalysis<T> pa,",
            "        Map<CGNode, OrdinalSet<PointerKey>> mod,",
            "        ExplodedControlFlowGraph cfg,",
            "        Map<Integer, NormalStatement> ssaInstructionIndex2Statement) {",
            "     return null;",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   @Nullable public IntSet getPredNodeNumbers(T node) throws UnimplementedError {",
            "       Assertions.UNREACHABLE();",
            "       return null;",
            "   }",
            "  @Nullable OrdinalSet<Statement> computeResult(",
            "        Statement s,",
            "        Map<PointerKey, MutableIntSet> pointerKeyMod,",
            "        BitVectorSolver<? extends ISSABasicBlock> solver,",
            "        OrdinalSetMapping<Statement> domain,",
            "        CGNode node,",
            "        ExtendedHeapModel h,",
            "        PointerAnalysis<T> pa,",
            "        Map<CGNode, OrdinalSet<PointerKey>> mod,",
            "        ExplodedControlFlowGraph cfg,",
            "        Map<Integer, NormalStatement> ssaInstructionIndex2Statement) {",
            "     return null;",
            "   }",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnMethod("Foo.java", "test.Foo", "getPredNodeNumbers(T)"),
                "javax.annotation.Nullable"),
            new AddMarkerAnnotation(
                new OnMethod(
                    "Foo.java",
                    "test.Foo",
                    "computeResult(com.ibm.wala.ipa.slicer.Statement,java.util.Map,com.ibm.wala.dataflow.graph.BitVectorSolver,com.ibm.wala.util.intset.OrdinalSetMapping,com.ibm.wala.ipa.callgraph.CGNode,com.ibm.wala.ipa.modref.ExtendedHeapModel,com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis,java.util.Map,com.ibm.wala.ssa.analysis.ExplodedControlFlowGraph,java.util.Map)"),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void methodNullableSignatureArrayBrackets() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "public class Foo {",
            "   protected CGNode getTargetForCall(",
            "     CGNode caller[], CallSiteReference[][][] site, IClass recv, InstanceKey[][] iKey) {",
            "     return null;",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   @Nullable protected CGNode getTargetForCall(",
            "     CGNode caller[], CallSiteReference[][][] site, IClass recv, InstanceKey[][] iKey) {",
            "     return null;",
            "   }",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnMethod(
                    "Foo.java",
                    "test.Foo",
                    "getTargetForCall(com.ibm.wala.ipa.callgraph.CGNode[],com.ibm.wala.classLoader.CallSiteReference[][][],com.ibm.wala.classLoader.IClass,com.ibm.wala.ipa.callgraph.propagation.InstanceKey[][])"),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void methodNullableSignatureGenericMethodName() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "public class Foo {",
            "   static <T> T getReader(ClassReader.AttrIterator iter, String attrName, GetReader<T> reader) {",
            "     return null;",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   @Nullable static <T> T getReader(ClassReader.AttrIterator iter, String attrName, GetReader<T> reader) {",
            "     return null;",
            "   }",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnMethod(
                    "Foo.java",
                    "test.Foo",
                    "<T>getReader(com.ibm.wala.shrikeCT.ClassReader.AttrIterator,java.lang.String,com.ibm.wala.classLoader.ShrikeClass.GetReader)"),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void methodNullableDotArray() {
    injectorTestHelper
        .addInput(
            "Main.java",
            "package test;",
            "public class Main {",
            "   public void format(String type,Object... objs) {",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import javax.annotation.Initializer;",
            "public class Main {",
            "   @Initializer public void format(String type,Object... objs) {",
            "   }",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnMethod(
                    "Main.java", "test.Main", "format(java.lang.String,java.lang.Object[])"),
                "javax.annotation.Initializer"))
        .start();
  }

  @Test
  public void methodWithMultipleLineDeclaration() {
    injectorTestHelper
        .addInput(
            "Main.java",
            "package edu.ucr;",
            "public class Main {",
            "   @Foo(clazz = String.class, value = \"Some description\")",
            "   private static @Baz Object foo6(",
            "       final @Baz Map<String, @Baz Object> m,",
            "       @Foo(clazz = String.class, value = \"Some argument\") @Baz Object o1,",
            "       Object o2,",
            "       Object o3,",
            "       @Foo(clazz = Object.class, value = \"Some other argument\") @Baz Object o4) {",
            "     return new @Baz Runnable() {",
            "       public void run() {",
            "         System.out.print(\"log\");",
            "       }",
            "     };",
            "   }",
            "}")
        .expectOutput(
            "package edu.ucr;",
            "import javax.annotation.Nullable;",
            "public class Main {",
            "   @Nullable @Foo(clazz = String.class, value = \"Some description\")",
            "   private static @Baz Object foo6(",
            "       final @Baz Map<String, @Baz Object> m,",
            "       @Foo(clazz = String.class, value = \"Some argument\") @Baz Object o1,",
            "       Object o2,",
            "       Object o3,",
            "       @Foo(clazz = Object.class, value = \"Some other argument\") @Baz Object o4) {",
            "     return new @Baz Runnable() {",
            "       public void run() {",
            "         System.out.print(\"log\");",
            "       }",
            "     };",
            "   }",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnMethod(
                    "Main.java", "edu.ucr.Main", "foo6(Map, Object, Object, Object, Object)"),
                "javax.annotation.Nullable"))
        .start();
  }
}
