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

import edu.ucr.cs.riple.injector.location.OnParameter;
import edu.ucr.cs.riple.injector.tools.InjectorTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class OnParameterInjectionTest {
  @Test
  public void parameter_nullable_simple() {
    String rootName = "parameter_nullable_simple";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable Object test(Object flag) {",
            "       if(flag == null) {",
            "           return new Object();",
            "       }",
            "       else return new Object();",
            "   }",
            "}")
        .expectOutput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable Object test(@Nullable Object flag) {",
            "       if(flag == null) {",
            "           return new Object();",
            "       }",
            "       else return new Object();",
            "   }",
            "}")
        .addChanges(
            new Change(
                new OnParameter(
                    "Super.java", "com.uber.Super", "test(java.lang.Object)", "flag", 0),
                "javax.annotation.Nullable",
                true))
        .start();
  }

  @Test
  public void parameter_nullable_signature_incomplete() {
    String rootName = "parameter_nullable_signature_incomplete";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable Object test(Object flag) {",
            "       if(flag == null) {",
            "           return new Object();",
            "       }",
            "       else return new Object();",
            "   }",
            "}")
        .expectOutput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable Object test(@Nullable Object flag) {",
            "       if(flag == null) {",
            "           return new Object();",
            "       }",
            "       else return new Object();",
            "   }",
            "}")
        .addChanges(
            new Change(
                new OnParameter("Super.java", "com.uber.Super", "test(Object)", "flag", 1),
                "javax.annotation.Nullable",
                true))
        .start();
  }

  @Test
  public void parameter_nullable_interface() {
    String rootName = "parameter_nullable_interface";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "SSAInstructionFactory.java",
            "package com.uber;",
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
            "SSAInstructionFactory.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public interface SSAInstructionFactory {",
            "SSAAbstractInvokeInstruction InvokeInstruction(",
            "   int index,",
            "   int result,",
            "   int[] params,",
            "   int exception,",
            "   CallSiteReference site,",
            "   @Nullable BootstrapMethod bootstrap);",
            "",
            "SSAAbstractInvokeInstruction InvokeInstruction(",
            "   int index, int[] params, int exception, CallSiteReference site, BootstrapMethod bootstrap);",
            "}")
        .addChanges(
            new Change(
                new OnParameter(
                    "SSAInstructionFactory.java",
                    "com.uber.SSAInstructionFactory",
                    "InvokeInstruction(int,int,int[],int,com.ibm.wala.classLoader.CallSiteReference,com.ibm.wala.shrikeCT.BootstrapMethodsReader.BootstrapMethod)",
                    "bootstrap",
                    5),
                "javax.annotation.Nullable",
                true))
        .start();
  }

  @Test
  public void parameter_nullable_generics_simple() {
    String rootName = "parameter_nullable_generics_simple";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "ModRef.java",
            "package com.uber;",
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
            "ModRef.java",
            "package com.uber;",
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
            new Change(
                new OnParameter(
                    "ModRef.java",
                    "com.uber.ModRef",
                    "computeMod(com.ibm.wala.ipa.callgraph.CallGraph,com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis<T>,com.ibm.wala.ipa.slicer.HeapExclusions)",
                    "heapExclude",
                    2),
                "javax.annotation.Nullable",
                true))
        .start();
  }

  @Test
  public void parameter_nullable_generics_multiple() {
    String rootName = "parameter_nullable_generics_multiple";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "ModRef.java",
            "package com.uber;",
            "public class ModRef {",
            "   public ModRef(",
            "       IMethod method,",
            "       Context context,",
            "       AbstractCFG<?, ?> cfg,",
            "       SSAInstruction[] instructions,",
            "       SSAOptions options,",
            "       Map<Integer, ConstantValue> constants)",
            "       throws AssertionError {",
            "           super(",
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
            "ModRef.java",
            "package com.uber;",
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
            "           super(",
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
            new Change(
                new OnParameter(
                    "ModRef.java",
                    "com.uber.ModRef",
                    "ModRef(com.ibm.wala.classLoader.IMethod,com.ibm.wala.ipa.callgraph.Context,com.ibm.wala.cfg.AbstractCFG<?,?>,com.ibm.wala.ssa.SSAInstruction[],com.ibm.wala.ssa.SSAOptions,java.util.Map<java.lang.Integer,com.ibm.wala.ssa.ConstantValue>)",
                    "constants",
                    5),
                "javax.annotation.Nullable",
                true))
        .start();
  }

  @Test
  public void parameter_nullable_with_annotation() {
    String rootName = "parameter_nullable_with_annotation";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "WeakKeyReference.java",
            "package com.uber;",
            "class WeakKeyReference<K> extends WeakReference<K> implements InternalReference<K> {",
            "   private final int hashCode;",
            "   public WeakKeyReference(@Nullable K key, ReferenceQueue<K> queue) {",
            "     super(key, queue);",
            "     hashCode = System.identityHashCode(key);",
            "   }",
            "}")
        .expectOutput(
            "WeakKeyReference.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "class WeakKeyReference<K> extends WeakReference<K> implements InternalReference<K> {",
            "   private final int hashCode;",
            "   public WeakKeyReference(@Nullable K key, @Nullable ReferenceQueue<K> queue) {",
            "     super(key, queue);",
            "     hashCode = System.identityHashCode(key);",
            "   }",
            "}")
        .addChanges(
            new Change(
                new OnParameter(
                    "WeakKeyReference.java",
                    "com.uber.WeakKeyReference",
                    "WeakKeyReference(@org.checkerframework.checker.nullness.qual.Nullable K,java.lang.ref.ReferenceQueue<K>)",
                    "queue",
                    1),
                "javax.annotation.Nullable",
                true))
        .start();
  }
}