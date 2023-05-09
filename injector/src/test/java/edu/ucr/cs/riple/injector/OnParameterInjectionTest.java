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
import edu.ucr.cs.riple.injector.changes.RemoveMarkerAnnotation;
import edu.ucr.cs.riple.injector.location.OnParameter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class OnParameterInjectionTest extends BaseInjectorTest {
  @Test
  public void parameterNullableSimple() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   @Nullable Object test(Object flag) {",
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
            "   @Nullable Object test(@Nullable Object flag) {",
            "       if(flag == null) {",
            "           return new Object();",
            "       }",
            "       else return new Object();",
            "   }",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnParameter("Foo.java", "test.Foo", "test(java.lang.Object)", 0),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void parameterNullableSignatureIncomplete() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   @Nullable Object test(Object flag) {",
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
            "   @Nullable Object test(@Nullable Object flag) {",
            "       if(flag == null) {",
            "           return new Object();",
            "       }",
            "       else return new Object();",
            "   }",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnParameter("Foo.java", "test.Foo", "test(Object)", 0),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void parameterNullableInterface() {
    injectorTestHelper
        .addInput(
            "SSAInstructionFactory.java",
            "package test;",
            "public interface SSAInstructionFactory {",
            "SSAAbstractInvokeInstruction InvokeInstruction(",
            "   int index,",
            "   int result,",
            "   int[] params,",
            "   int exception,",
            "   CallSiteReference site,",
            "   BootstrapMethod bootstrap);",
            "SSAAbstractInvokeInstruction InvokeInstruction(",
            "   int index, int[] params, int exception, CallSiteReference site, BootstrapMethod bootstrap);",
            "}")
        .expectOutput(
            "package test;",
            "import javax.annotation.Nullable;",
            "public interface SSAInstructionFactory {",
            "SSAAbstractInvokeInstruction InvokeInstruction(",
            "   int index,",
            "   int result,",
            "   int[] params,",
            "   int exception,",
            "   CallSiteReference site,",
            "   @Nullable BootstrapMethod bootstrap);",
            "SSAAbstractInvokeInstruction InvokeInstruction(",
            "   int index, int[] params, int exception, CallSiteReference site, BootstrapMethod bootstrap);",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnParameter(
                    "SSAInstructionFactory.java",
                    "test.SSAInstructionFactory",
                    "InvokeInstruction(int,int,int[],int,com.ibm.wala.classLoader.CallSiteReference,com.ibm.wala.shrikeCT.BootstrapMethodsReader.BootstrapMethod)",
                    5),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void parameterNullableGenericsSimple() {
    injectorTestHelper
        .addInput(
            "ModRef.java",
            "package test;",
            "public class ModRef<T extends InstanceKey> {",
            "   public Map<CGNode, OrdinalSet<PointerKey>> computeMod(",
            "     CallGraph cg, PointerAnalysis<T> pa, HeapExclusions heapExclude) {",
            "     if (cg == null) {",
            "       throw new IllegalArgumentException(\"cg is null\");",
            "     }",
            "     Map<CGNode, Collection<PointerKey>> scan = scanForMod(cg, pa, heapExclude);",
            "     return CallGraphTransitiveClosure.transitiveClosure(cg, scan);",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import javax.annotation.Nullable;",
            "public class ModRef<T extends InstanceKey> {",
            "   public Map<CGNode, OrdinalSet<PointerKey>> computeMod(",
            "     CallGraph cg, PointerAnalysis<T> pa, @Nullable HeapExclusions heapExclude) {",
            "     if (cg == null) {",
            "       throw new IllegalArgumentException(\"cg is null\");",
            "     }",
            "     Map<CGNode, Collection<PointerKey>> scan = scanForMod(cg, pa, heapExclude);",
            "     return CallGraphTransitiveClosure.transitiveClosure(cg, scan);",
            "   }",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnParameter(
                    "ModRef.java",
                    "test.ModRef",
                    "computeMod(com.ibm.wala.ipa.callgraph.CallGraph,com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis,com.ibm.wala.ipa.slicer.HeapExclusions)",
                    2),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void parameterNullableGenericsMultiple() {
    injectorTestHelper
        .addInput(
            "ModRef.java",
            "package test;",
            "public class ModRef {",
            "   public ModRef(",
            "       IMethod method,",
            "       Context context,",
            "       AbstractCFG<?, ?> cfg,",
            "       SSAInstruction[] instructions,",
            "       SSAOptions options,",
            "       Map<Integer, ConstantValue> constants)",
            "       throws AssertionError {",
            "           Foo(",
            "               method, ",
            "               instructions,",
            "               makeSymbolTable(method, instructions, constants, cfg),",
            "               new SSACFG(method, cfg, instructions),",
            "               options",
            "           );",
            "         if (PARANOID) { repOK(instructions); }",
            "         setupLocationMap();",
            "    }",
            "}")
        .expectOutput(
            "package test;",
            "import javax.annotation.Nullable;",
            "public class ModRef {",
            "   public ModRef(",
            "       IMethod method,",
            "       Context context,",
            "       AbstractCFG<?, ?> cfg,",
            "       SSAInstruction[] instructions,",
            "       SSAOptions options,",
            "       @Nullable Map<Integer, ConstantValue> constants)",
            "       throws AssertionError {",
            "           Foo(",
            "               method, ",
            "               instructions,",
            "               makeSymbolTable(method, instructions, constants, cfg),",
            "               new SSACFG(method, cfg, instructions),",
            "               options",
            "           );",
            "         if (PARANOID) { repOK(instructions); }",
            "         setupLocationMap();",
            "    }",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnParameter(
                    "ModRef.java",
                    "test.ModRef",
                    "ModRef(com.ibm.wala.classLoader.IMethod,com.ibm.wala.ipa.callgraph.Context,com.ibm.wala.cfg.AbstractCFG,com.ibm.wala.ssa.SSAInstruction[],com.ibm.wala.ssa.SSAOptions,java.util.Map)",
                    5),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void parameterNullableWithAnnotation() {
    injectorTestHelper
        .addInput(
            "WeakKeyReference.java",
            "package test;",
            "class WeakKeyReference<K> extends WeakReference<K> implements InternalReference<K> {",
            "   private final int hashCode;",
            "   public WeakKeyReference(@Nullable K key, ReferenceQueue<K> queue) {",
            "     Foo(key, queue);",
            "     hashCode = System.identityHashCode(key);",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import javax.annotation.Nullable;",
            "class WeakKeyReference<K> extends WeakReference<K> implements InternalReference<K> {",
            "   private final int hashCode;",
            "   public WeakKeyReference(@Nullable K key, @Nullable ReferenceQueue<K> queue) {",
            "     Foo(key, queue);",
            "     hashCode = System.identityHashCode(key);",
            "   }",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnParameter(
                    "WeakKeyReference.java",
                    "test.WeakKeyReference",
                    "WeakKeyReference(K,java.lang.ref.ReferenceQueue)",
                    1),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void methodWithMultipleLineDeclarationFirstParam() {
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
            "   @Foo(clazz = String.class, value = \"Some description\")",
            "   private static @Baz Object foo6(",
            "       @Nullable final @Baz Map<String, @Baz Object> m,",
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
                new OnParameter(
                    "Main.java", "edu.ucr.Main", "foo6(Map, Object, Object, Object, Object)", 0),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void methodWithMultipleLineDeclarationSecondParam() {
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
            "   @Foo(clazz = String.class, value = \"Some description\")",
            "   private static @Baz Object foo6(",
            "       final @Baz Map<String, @Baz Object> m,",
            "       @Nullable @Foo(clazz = String.class, value = \"Some argument\") @Baz Object o1,",
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
                new OnParameter(
                    "Main.java", "edu.ucr.Main", "foo6(Map, Object, Object, Object, Object)", 1),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void methodWithMultipleLineDeclarationFifthParam() {
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
            "   @Foo(clazz = String.class, value = \"Some description\")",
            "   private static @Baz Object foo6(",
            "       final @Baz Map<String, @Baz Object> m,",
            "       @Foo(clazz = String.class, value = \"Some argument\") @Baz Object o1,",
            "       Object o2,",
            "       Object o3,",
            "       @Nullable @Foo(clazz = Object.class, value = \"Some other argument\") @Baz Object o4) {",
            "     return new @Baz Runnable() {",
            "       public void run() {",
            "         System.out.print(\"log\");",
            "       }",
            "     };",
            "   }",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnParameter(
                    "Main.java", "edu.ucr.Main", "foo6(Map, Object, Object, Object, Object)", 4),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void methodWithMultipleLineInlineMultiParameterDeclarationSecondParam() {
    injectorTestHelper
        .addInput(
            "Main.java",
            "package edu.ucr;",
            "import javax.annotation.Nullable;",
            "public class Main {",
            "   @Foo(clazz = String.class, value = \"Some description\")",
            "   private static @Baz Object foo6(",
            "       final @Baz Map<String, @Baz Object> m,",
            "       @Foo(clazz = String.class, value = \"Some argument\") @Baz Object o1, Object o2,",
            "       Object o3, @Foo(clazz = Object.class, value = \"Some other argument\") @Baz Object o4) {",
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
            "   @Foo(clazz = String.class, value = \"Some description\")",
            "   private static @Baz Object foo6(",
            "       final @Baz Map<String, @Baz Object> m,",
            "       @Nullable @Foo(clazz = String.class, value = \"Some argument\") @Baz Object o1, Object o2,",
            "       Object o3, @Foo(clazz = Object.class, value = \"Some other argument\") @Baz Object o4) {",
            "     return new @Baz Runnable() {",
            "       public void run() {",
            "         System.out.print(\"log\");",
            "       }",
            "     };",
            "   }",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnParameter(
                    "Main.java", "edu.ucr.Main", "foo6(Map, Object, Object, Object, Object)", 1),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void methodWithMultipleLineInlineMultiParameterDeclarationFifthParam() {
    injectorTestHelper
        .addInput(
            "Main.java",
            "package edu.ucr;",
            "import javax.annotation.Nullable;",
            "public class Main {",
            "   @Foo(clazz = String.class, value = \"Some description\")",
            "   private static @Baz Object foo6(",
            "       final @Baz Map<String, @Baz Object> m,",
            "       @Foo(clazz = String.class, value = \"Some argument\") @Baz Object o1, Object o2,",
            "       Object o3, @Foo(clazz = Object.class, value = \"Some other argument\") @Baz Object o4) {",
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
            "   @Foo(clazz = String.class, value = \"Some description\")",
            "   private static @Baz Object foo6(",
            "       final @Baz Map<String, @Baz Object> m,",
            "       @Foo(clazz = String.class, value = \"Some argument\") @Baz Object o1, Object o2,",
            "       Object o3, @Nullable @Foo(clazz = Object.class, value = \"Some other argument\") @Baz Object o4) {",
            "     return new @Baz Runnable() {",
            "       public void run() {",
            "         System.out.print(\"log\");",
            "       }",
            "     };",
            "   }",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnParameter(
                    "Main.java", "edu.ucr.Main", "foo6(Map, Object, Object, Object, Object)", 4),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void methodWithMultipleLineInlineMultiParameterDeclarationFirstParamWithModifierRemoval() {
    injectorTestHelper
        .addInput(
            "Main.java",
            "package edu.ucr;",
            "import javax.annotation.Nullable;",
            "public class Main {",
            "   @Foo(clazz = String.class, value = \"Some description\")",
            "   private static @Baz Object foo6(",
            "       @Nullable final @Baz Map<String, @Baz Object> m,",
            "       @Foo(clazz = String.class, value = \"Some argument\") @Baz Object o1, Object o2,",
            "       Object o3, @Foo(clazz = Object.class, value = \"Some other argument\") @Baz Object o4) {",
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
            "   @Foo(clazz = String.class, value = \"Some description\")",
            "   private static @Baz Object foo6(",
            "       final @Baz Map<String, @Baz Object> m,",
            "       @Foo(clazz = String.class, value = \"Some argument\") @Baz Object o1, Object o2,",
            "       Object o3, @Foo(clazz = Object.class, value = \"Some other argument\") @Baz Object o4) {",
            "     return new @Baz Runnable() {",
            "       public void run() {",
            "         System.out.print(\"log\");",
            "       }",
            "     };",
            "   }",
            "}")
        .addChanges(
            new RemoveMarkerAnnotation(
                new OnParameter(
                    "Main.java", "edu.ucr.Main", "foo6(Map, Object, Object, Object, Object)", 0),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void methodWithMultipleLineInlineMultiParameterDeclarationFifthParamRemoval() {
    injectorTestHelper
        .addInput(
            "Main.java",
            "package edu.ucr;",
            "import javax.annotation.Nullable;",
            "public class Main {",
            "   @Foo(clazz = String.class, value = \"Some description\")",
            "   private static @Baz Object foo6(",
            "       final @Baz Map<String, @Baz Object> m,",
            "       @Foo(clazz = String.class, value = \"Some argument\") @Baz Object o1, Object o2,",
            "       Object o3, @Nullable @Foo(clazz = Object.class, value = \"Some other argument\") @Baz Object o4) {",
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
            "   @Foo(clazz = String.class, value = \"Some description\")",
            "   private static @Baz Object foo6(",
            "       final @Baz Map<String, @Baz Object> m,",
            "       @Foo(clazz = String.class, value = \"Some argument\") @Baz Object o1, Object o2,",
            "       Object o3, @Foo(clazz = Object.class, value = \"Some other argument\") @Baz Object o4) {",
            "     return new @Baz Runnable() {",
            "       public void run() {",
            "         System.out.print(\"log\");",
            "       }",
            "     };",
            "   }",
            "}")
        .addChanges(
            new RemoveMarkerAnnotation(
                new OnParameter(
                    "Main.java", "edu.ucr.Main", "foo6(Map, Object, Object, Object, Object)", 4),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void methodWithMultipleLineInlineMultiParameterDeclarationAllParam() {
    injectorTestHelper
        .addInput(
            "Main.java",
            "package edu.ucr;",
            "import javax.annotation.Nullable;",
            "public class Main {",
            "   @Foo(clazz = String.class, value = \"Some description\")",
            "   private static @Baz Object foo6(",
            "       final @Baz Map<String, @Baz Object> m,",
            "       @Foo(clazz = String.class, value = \"Some argument\") @Baz Object o1, Object o2,",
            "       Object o3, @Foo(clazz = Object.class, value = \"Some other argument\") @Baz Object o4) {",
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
            "   @Foo(clazz = String.class, value = \"Some description\")",
            "   private static @Baz Object foo6(",
            "       @Nullable final @Baz Map<String, @Baz Object> m,",
            "       @Nullable @Foo(clazz = String.class, value = \"Some argument\") @Baz Object o1, @Nullable Object o2,",
            "       @Nullable Object o3, @Nullable @Foo(clazz = Object.class, value = \"Some other argument\") @Baz Object o4) {",
            "     return new @Baz Runnable() {",
            "       public void run() {",
            "         System.out.print(\"log\");",
            "       }",
            "     };",
            "   }",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnParameter(
                    "Main.java", "edu.ucr.Main", "foo6(Map, Object, Object, Object, Object)", 0),
                "javax.annotation.Nullable"),
            new AddMarkerAnnotation(
                new OnParameter(
                    "Main.java", "edu.ucr.Main", "foo6(Map, Object, Object, Object, Object)", 1),
                "javax.annotation.Nullable"),
            new AddMarkerAnnotation(
                new OnParameter(
                    "Main.java", "edu.ucr.Main", "foo6(Map, Object, Object, Object, Object)", 2),
                "javax.annotation.Nullable"),
            new AddMarkerAnnotation(
                new OnParameter(
                    "Main.java", "edu.ucr.Main", "foo6(Map, Object, Object, Object, Object)", 3),
                "javax.annotation.Nullable"),
            new AddMarkerAnnotation(
                new OnParameter(
                    "Main.java", "edu.ucr.Main", "foo6(Map, Object, Object, Object, Object)", 4),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void methodWithMultipleLineInlineMultiParameterDeclarationAllParamRemoval() {
    injectorTestHelper
        .addInput(
            "Main.java",
            "package edu.ucr;",
            "import javax.annotation.Nullable;",
            "public class Main {",
            "   @Foo(clazz = String.class, value = \"Some description\")",
            "   private static @Baz Object foo6(",
            "       @Nullable final @Baz Map<String, @Baz Object> m,",
            "       @Nullable @Foo(clazz = String.class, value = \"Some argument\") @Baz Object o1, @Nullable Object o2,",
            "       @Nullable Object o3, @Nullable @Foo(clazz = Object.class, value = \"Some other argument\") @Baz Object o4) {",
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
            "   @Foo(clazz = String.class, value = \"Some description\")",
            "   private static @Baz Object foo6(",
            "       final @Baz Map<String, @Baz Object> m,",
            "       @Foo(clazz = String.class, value = \"Some argument\") @Baz Object o1, Object o2,",
            "       Object o3, @Foo(clazz = Object.class, value = \"Some other argument\") @Baz Object o4) {",
            "     return new @Baz Runnable() {",
            "       public void run() {",
            "         System.out.print(\"log\");",
            "       }",
            "     };",
            "   }",
            "}")
        .addChanges(
            new RemoveMarkerAnnotation(
                new OnParameter(
                    "Main.java", "edu.ucr.Main", "foo6(Map, Object, Object, Object, Object)", 0),
                "javax.annotation.Nullable"),
            new RemoveMarkerAnnotation(
                new OnParameter(
                    "Main.java", "edu.ucr.Main", "foo6(Map, Object, Object, Object, Object)", 1),
                "javax.annotation.Nullable"),
            new RemoveMarkerAnnotation(
                new OnParameter(
                    "Main.java", "edu.ucr.Main", "foo6(Map, Object, Object, Object, Object)", 2),
                "javax.annotation.Nullable"),
            new RemoveMarkerAnnotation(
                new OnParameter(
                    "Main.java", "edu.ucr.Main", "foo6(Map, Object, Object, Object, Object)", 3),
                "javax.annotation.Nullable"),
            new RemoveMarkerAnnotation(
                new OnParameter(
                    "Main.java", "edu.ucr.Main", "foo6(Map, Object, Object, Object, Object)", 4),
                "javax.annotation.Nullable"))
        .start();
  }
}
