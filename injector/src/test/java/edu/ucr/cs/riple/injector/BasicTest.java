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

import edu.ucr.cs.riple.injector.tools.InjectorTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class BasicTest {
  InjectorTestHelper injectorTestHelper;

  @Before
  public void setup() {}

  @Test
  public void return_nullable_simple() {
    String rootName = "return_nullable_simple";

    injectorTestHelper =
        new InjectorTestHelper()
            .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
            .addInput(
                "Super.java",
                "package com.uber;",
                "public class Super {",
                "   Object test(boolean flag) {",
                "       return new Object();",
                "   }",
                "}")
            .expectOutput(
                "Super.java",
                "package com.uber;",
                "import javax.annotation.Nullable;",
                "public class Super {",
                "   @Nullable",
                "   Object test(boolean flag) {",
                "       return new Object();",
                "   }",
                "}")
            .addInput(
                "com/Superb.java",
                "package com.uber;",
                "public class Superb extends Super {",
                "   Object test(boolean flag) {",
                "       return new Object();",
                "   }",
                "}")
            .expectOutput(
                "com/Superb.java",
                "package com.uber;",
                "import javax.annotation.Nullable;",
                "public class Superb extends Super{",
                "   @Nullable",
                "   Object test(boolean flag) {",
                "       return new Object();",
                "   }",
                "}")
            .addFixes(
                new Fix(
                    "javax.annotation.Nullable",
                    "test(boolean)",
                    "",
                    "METHOD",
                    "com.uber.Super",
                    "Super.java",
                    "true"),
                new Fix(
                    "javax.annotation.Nullable",
                    "test(boolean)",
                    "",
                    "METHOD",
                    "com.uber.Superb",
                    "com/Superb.java",
                    "true"));
    injectorTestHelper.start();
    injectorTestHelper = null;
  }

  @Test
  public void return_nullable_enum_simple() {
    String rootName = "return_nullable_enum_simple";

    injectorTestHelper =
        new InjectorTestHelper()
            .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
            .addInput(
                "Main.java",
                "package com.uber;",
                "public class Main {",
                "   public enum Test{",
                "     CLASSIC;",
                "     public Object run(){",
                "       return null;",
                "     }",
                "   }",
                "}")
            .expectOutput(
                "Main.java",
                "package com.uber;",
                "import javax.annotation.Nullable;",
                "public class Main {",
                "   public enum Test{",
                "     CLASSIC;",
                "     @Nullable",
                "     public Object run(){",
                "       return null;",
                "     }",
                "   }",
                "}")
            .addFixes(
                new Fix(
                    "javax.annotation.Nullable",
                    "run()",
                    "",
                    "METHOD",
                    "com.uber.Main$Test",
                    "Main.java",
                    "true"));
    injectorTestHelper.start();
  }

  @Test
  public void return_nullable_inner_class() {
    String rootName = "return_nullable_inner_class";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable",
            "   Object test(boolean flag) {",
            "       return new Object();",
            "   }",
            "   class SuperInner {",
            "       Object bar(@Nullable Object foo) {",
            "           return foo;",
            "       }",
            "   }",
            "}")
        .expectOutput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable",
            "   Object test(boolean flag) {",
            "       return new Object();",
            "   }",
            "   class SuperInner {",
            "       @Nullable",
            "       Object bar(@Nullable Object foo) {",
            "           return foo;",
            "       }",
            "   }",
            "}")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "bar(java.lang.Object)",
                "",
                "METHOD",
                "com.uber.Super$SuperInner",
                "Super.java",
                "true"))
        .start();
  }

  @Test
  public void return_nullable_class_decl_in_method() {
    String rootName = "return_nullable_class_decl_in_method";
    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Main.java",
            "package com.uber;",
            "public class Main {",
            "   void run() {",
            "       class Helper {",
            "         public Object run() { return null; }",
            "       }",
            "   }",
            "}")
        .expectOutput(
            "Main.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Main {",
            "   void run() {",
            "       class Helper {",
            "         @Nullable public Object run() { return null; }",
            "       }",
            "   }",
            "}")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "run()",
                "null",
                "METHOD",
                "com.uber.Main$1Helper",
                "Main.java",
                "true"))
        .start();
  }

  @Test
  public void return_nullable_signature_duplicate_type() {
    String rootName = "return_nullable_signature_duplicate_type";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "public class Super {",
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
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
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
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "test(Object, String, String)",
                "",
                "METHOD",
                "com.uber.Super",
                "Super.java",
                "true"),
            new Fix(
                "javax.annotation.Nullable",
                "test(Object, Object, String)",
                "name",
                "PARAMETER",
                "com.uber.Super",
                "Super.java",
                "true"))
        .start();
  }

  @Test
  public void return_nullable_single_generic_method_pick() {
    String rootName = "return_nullable_single_generic_method_pick";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "public class Super {",
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
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable",
            "   public IntSet getPredNodeNumbers(T node) throws UnimplementedError {",
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
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "getPredNodeNumbers(T)",
                "",
                "METHOD",
                "com.uber.Super",
                "Super.java",
                "true"),
            new Fix(
                "javax.annotation.Nullable",
                "computeResult(com.ibm.wala.ipa.slicer.Statement,java.util.Map<com.ibm.wala.ipa.callgraph.propagation.PointerKey,com.ibm.wala.util.intset.MutableIntSet>,com.ibm.wala.dataflow.graph.BitVectorSolver<? extends com.ibm.wala.ssa.ISSABasicBlock>,com.ibm.wala.util.intset.OrdinalSetMapping<com.ibm.wala.ipa.slicer.Statement>,com.ibm.wala.ipa.callgraph.CGNode,com.ibm.wala.ipa.modref.ExtendedHeapModel,com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis<T>,java.util.Map<com.ibm.wala.ipa.callgraph.CGNode,com.ibm.wala.util.intset.OrdinalSet<com.ibm.wala.ipa.callgraph.propagation.PointerKey>>,com.ibm.wala.ssa.analysis.ExplodedControlFlowGraph,java.util.Map<java.lang.Integer,com.ibm.wala.ipa.slicer.NormalStatement>)",
                "",
                "METHOD",
                "com.uber.Super",
                "Super.java",
                "true"))
        .start();
  }

  @Test
  public void return_nullable_signature_array_brackets() {
    String rootName = "return_nullable_signature_array_brackets";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "public class Super {",
            "   protected CGNode getTargetForCall(",
            "     CGNode caller[], CallSiteReference[][][] site, IClass recv, InstanceKey[][] iKey) {",
            "     return null;",
            "   }",
            "}")
        .expectOutput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable protected CGNode getTargetForCall(",
            "     CGNode caller[], CallSiteReference[][][] site, IClass recv, InstanceKey[][] iKey) {",
            "     return null;",
            "   }",
            "}")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "getTargetForCall(com.ibm.wala.ipa.callgraph.CGNode[],com.ibm.wala.classLoader.CallSiteReference[][][],com.ibm.wala.classLoader.IClass,com.ibm.wala.ipa.callgraph.propagation.InstanceKey[][])",
                "",
                "METHOD",
                "com.uber.Super",
                "Super.java",
                "true"))
        .start(true);
  }

  @Test
  public void return_nullable_signature_generic_method_name() {
    String rootName = "return_nullable_signature_generic_method_name";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "public class Super {",
            "   static <T> T getReader(ClassReader.AttrIterator iter, String attrName, GetReader<T> reader) {",
            "     return null;",
            "   }",
            "}")
        .expectOutput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable static <T> T getReader(ClassReader.AttrIterator iter, String attrName, GetReader<T> reader) {",
            "     return null;",
            "   }",
            "}")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "<T>getReader(com.ibm.wala.shrikeCT.ClassReader.AttrIterator,java.lang.String,com.ibm.wala.classLoader.ShrikeClass.GetReader<T>)",
                "",
                "METHOD",
                "com.uber.Super",
                "Super.java",
                "true"))
        .start();
  }

  @Test
  public void return_nullable_dot_array() {
    String rootName = "return_nullable_dot_array";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Main.java",
            "package com.uber;",
            "public class Main {",
            "   public void format(String type,Object... objs) {",
            "   }",
            "}")
        .expectOutput(
            "Main.java",
            "package com.uber;",
            "import javax.annotation.Initializer;",
            "public class Main {",
            "   @Initializer",
            "   public void format(String type,Object... objs) {",
            "   }",
            "}")
        .addFixes(
            new Fix(
                "javax.annotation.Initializer",
                "format(java.lang.String,java.lang.Object...)",
                "",
                "METHOD",
                "com.uber.Main",
                "Main.java",
                "true"))
        .start();
  }

  @Test
  public void initializer_constructor() {
    String rootName = "initializer_constructor";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Main.java",
            "package com.uber;",
            "public class Main {",
            "   public Main(String type,Object... objs) {",
            "   }",
            "}")
        .expectOutput(
            "Main.java",
            "package com.uber;",
            "import javax.annotation.Initializer;",
            "public class Main {",
            "   @Initializer",
            "   public Main(String type, Object... objs) {",
            "   }",
            "}")
        .addFixes(
            new Fix(
                "javax.annotation.Initializer",
                "Main(java.lang.String,java.lang.Object...)",
                "",
                "METHOD",
                "com.uber.Main",
                "Main.java",
                "true"))
        .start();
  }

  @Test
  public void param_nullable_simple() {
    String rootName = "param_nullable_simple";

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
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "test(java.lang.Object)",
                "flag",
                "PARAMETER",
                "com.uber.Super",
                "Super.java",
                "true"))
        .start();
  }

  @Test
  public void param_nullable_signature_incomplete() {
    String rootName = "param_nullable_signature_incomplete";

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
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "test(Object)",
                "flag",
                "PARAMETER",
                "com.uber.Super",
                "Super.java",
                "true"))
        .start();
  }

  @Test
  public void param_nullable_interface() {
    String rootName = "param_nullable_interface";

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
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "InvokeInstruction(int,int,int[],int,com.ibm.wala.classLoader.CallSiteReference,com.ibm.wala.shrikeCT.BootstrapMethodsReader.BootstrapMethod)",
                "bootstrap",
                "PARAMETER",
                "com.uber.SSAInstructionFactory",
                "SSAInstructionFactory.java",
                "true"))
        .start();
  }

  @Test
  public void param_nullable_generics_simple() {
    String rootName = "param_nullable_generics_simple";

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
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "computeMod(com.ibm.wala.ipa.callgraph.CallGraph,com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis<T>,com.ibm.wala.ipa.slicer.HeapExclusions)",
                "heapExclude",
                "PARAMETER",
                "com.uber.ModRef",
                "ModRef.java",
                "true"))
        .start();
  }

  @Test
  public void param_nullable_generics_multiple() {
    String rootName = "param_nullable_generics_multiple";

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
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "ModRef(com.ibm.wala.classLoader.IMethod,com.ibm.wala.ipa.callgraph.Context,com.ibm.wala.cfg.AbstractCFG<?,?>,com.ibm.wala.ssa.SSAInstruction[],com.ibm.wala.ssa.SSAOptions,java.util.Map<java.lang.Integer,com.ibm.wala.ssa.ConstantValue>)",
                "constants",
                "PARAMETER",
                "com.uber.ModRef",
                "ModRef.java",
                "true"))
        .start();
  }

  @Test
  public void param_nullable_with_annotation() {
    String rootName = "param_nullable_with_annotation";

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
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "WeakKeyReference(@org.checkerframework.checker.nullness.qual.Nullable K,java.lang.ref.ReferenceQueue<K>)",
                "queue",
                "PARAMETER",
                "com.uber.WeakKeyReference",
                "WeakKeyReference.java",
                "true"))
        .start();
  }

  @Test
  public void field_nullable_simple() {
    String rootName = "field_nullable_simple";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   Object h = new Object();",
            "   public void test(@Nullable Object f) {",
            "      h = f;",
            "   }",
            "}")
        .expectOutput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable Object h = new Object();",
            "   public void test(@Nullable Object f) {",
            "      h = f;",
            "   }",
            "}")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "",
                "h",
                "FIELD",
                "com.uber.Super",
                "Super.java",
                "true"))
        .start();
  }

  @Test
  public void empty_PARAMETER_pick() {
    String rootName = "empty_PARAMETER_pick";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   Object test() {",
            "       return new Object();",
            "   }",
            "   class SuperInner {",
            "       Object bar(@Nullable Object foo) {",
            "           return foo;",
            "       }",
            "   }",
            "}")
        .expectOutput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable",
            "   Object test() {",
            "       return new Object();",
            "   }",
            "   class SuperInner {",
            "       Object bar(@Nullable Object foo) {",
            "           return foo;",
            "       }",
            "   }",
            "}")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "test()",
                "",
                "METHOD",
                "com.uber.Super",
                "Super.java",
                "true"))
        .start();
  }

  @Test
  public void skip_duplicate_annotation() {
    String rootName = "skip_duplicate_annotation";

    Fix fix =
        new Fix(
            "javax.annotation.Nullable",
            "test()",
            "",
            "METHOD",
            "com.uber.Super",
            "Super.java",
            "true");

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable Object test() {",
            "       return new Object();",
            "   }",
            "}")
        .expectOutput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable Object test() {",
            "       return new Object();",
            "   }",
            "}")
        .addFixes(fix, fix.duplicate(), fix.duplicate())
        .start();
  }

  @Test
  public void save_imports() {
    String rootName = "save_imports";
    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "import static com.ibm.wala.types.TypeName.ArrayMask;",
            "import static com.ibm.wala.types.TypeName.ElementBits;",
            "import static com.ibm.wala.types.TypeName.PrimitiveMask;",
            "import com.ibm.wala.types.TypeName.IntegerMask;",
            "import com.ibm.wala.util.collections.HashMapFactory;",
            "import java.io.Serializable;",
            "import java.util.Map;",
            "public class Super {",
            "   Object test() {",
            "       return new Object();",
            "   }",
            "}")
        .expectOutput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "import static com.ibm.wala.types.TypeName.ArrayMask;",
            "import static com.ibm.wala.types.TypeName.ElementBits;",
            "import static com.ibm.wala.types.TypeName.PrimitiveMask;",
            "import com.ibm.wala.types.TypeName.IntegerMask;",
            "import com.ibm.wala.util.collections.HashMapFactory;",
            "import java.io.Serializable;",
            "import java.util.Map;",
            "public class Super {",
            "   @Nullable Object test() {",
            "       return new Object();",
            "   }",
            "}")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "test()",
                "",
                "METHOD",
                "com.uber.Super",
                "Super.java",
                "true"))
        .start();
  }

  @Test
  public void save_imports_asterisk() {
    String rootName = "save_imports_asterisk";
    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "import static com.ibm.wala.types.A;",
            "import static com.ibm.wala.types.B;",
            "import static com.ibm.wala.types.C;",
            "import static com.ibm.wala.types.D;",
            "import static com.ibm.wala.types.E;",
            "import static com.ibm.wala.types.F;",
            "import static com.ibm.wala.types.G;",
            "import static com.ibm.wala.types.H;",
            "import static com.ibm.wala.types.I;",
            "import static com.ibm.wala.types.J;",
            "import static com.ibm.wala.types.K;",
            "import static com.ibm.wala.types.L;",
            "import com.ibm.wala.util.A;",
            "import com.ibm.wala.util.B;",
            "import com.ibm.wala.util.C;",
            "import com.ibm.wala.util.D;",
            "import com.ibm.wala.util.E;",
            "import com.ibm.wala.util.F;",
            "import com.ibm.wala.util.G;",
            "import com.ibm.wala.util.H;",
            "import com.ibm.wala.util.I;",
            "import com.ibm.wala.util.J;",
            "import com.ibm.wala.util.K;",
            "import com.ibm.wala.util.L;",
            "import com.ibm.wala.util.M;",
            "import com.ibm.wala.util.N;",
            "import com.ibm.wala.util.O;",
            "import com.ibm.wala.util.P;",
            "import com.ibm.wala.util.Q;",
            "import com.ibm.wala.util.R;",
            "import com.ibm.wala.util.S;",
            "import com.ibm.wala.util.T;",
            "import com.ibm.wala.util.U;",
            "import com.ibm.wala.util.V;",
            "import com.ibm.wala.util.W;",
            "import com.ibm.wala.util.X;",
            "import com.ibm.wala.util.Y;",
            "import com.ibm.wala.util.Z;",
            "import com.ibm.wala.util.AA;",
            "import com.ibm.wala.util.AB;",
            "public class Super {",
            "   A a = new A();",
            "   B b = new B();",
            "   C c = new C();",
            "   D d = new D();",
            "   E e = new E();",
            "   F f = new F();",
            "   G g = new G();",
            "   H h = new H();",
            "   I i = new I();",
            "   J j = new J();",
            "   K k = new K();",
            "   L l = new L();",
            "   M m = new M();",
            "   N n = new N();",
            "   P p = new P();",
            "   Q q = new Q();",
            "   R r = new R();",
            "   S s = new S();",
            "   T t = new T();",
            "   U u = new U();",
            "   V v = new V();",
            "   W w = new W();",
            "   X x = new X();",
            "   Y y = new Y();",
            "   Z z = new Z();",
            "   AA aa = new AA();",
            "   AB ab = new AB();",
            "   Object test() {",
            "       return new Object();",
            "   }",
            "}")
        .expectOutput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "import static com.ibm.wala.types.A;",
            "import static com.ibm.wala.types.B;",
            "import static com.ibm.wala.types.C;",
            "import static com.ibm.wala.types.D;",
            "import static com.ibm.wala.types.E;",
            "import static com.ibm.wala.types.F;",
            "import static com.ibm.wala.types.G;",
            "import static com.ibm.wala.types.H;",
            "import static com.ibm.wala.types.I;",
            "import static com.ibm.wala.types.J;",
            "import static com.ibm.wala.types.K;",
            "import static com.ibm.wala.types.L;",
            "import com.ibm.wala.util.A;",
            "import com.ibm.wala.util.B;",
            "import com.ibm.wala.util.C;",
            "import com.ibm.wala.util.D;",
            "import com.ibm.wala.util.E;",
            "import com.ibm.wala.util.F;",
            "import com.ibm.wala.util.G;",
            "import com.ibm.wala.util.H;",
            "import com.ibm.wala.util.I;",
            "import com.ibm.wala.util.J;",
            "import com.ibm.wala.util.K;",
            "import com.ibm.wala.util.L;",
            "import com.ibm.wala.util.M;",
            "import com.ibm.wala.util.N;",
            "import com.ibm.wala.util.O;",
            "import com.ibm.wala.util.P;",
            "import com.ibm.wala.util.Q;",
            "import com.ibm.wala.util.R;",
            "import com.ibm.wala.util.S;",
            "import com.ibm.wala.util.T;",
            "import com.ibm.wala.util.U;",
            "import com.ibm.wala.util.V;",
            "import com.ibm.wala.util.W;",
            "import com.ibm.wala.util.X;",
            "import com.ibm.wala.util.Y;",
            "import com.ibm.wala.util.Z;",
            "import com.ibm.wala.util.AA;",
            "import com.ibm.wala.util.AB;",
            "public class Super {",
            "   A a = new A();",
            "   B b = new B();",
            "   C c = new C();",
            "   D d = new D();",
            "   E e = new E();",
            "   F f = new F();",
            "   G g = new G();",
            "   H h = new H();",
            "   I i = new I();",
            "   J j = new J();",
            "   K k = new K();",
            "   L l = new L();",
            "   M m = new M();",
            "   N n = new N();",
            "   P p = new P();",
            "   Q q = new Q();",
            "   R r = new R();",
            "   S s = new S();",
            "   T t = new T();",
            "   U u = new U();",
            "   V v = new V();",
            "   W w = new W();",
            "   X x = new X();",
            "   Y y = new Y();",
            "   Z z = new Z();",
            "   AA aa = new AA();",
            "   AB ab = new AB();",
            "   @Nullable",
            "   Object test() {",
            "       return new Object();",
            "   }",
            "}")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "test()",
                "",
                "METHOD",
                "com.uber.Super",
                "Super.java",
                "true"))
        .start();
  }

  @Test
  public void remove_redundant_new_keyword() {
    String rootName = "remove_redundant_new_keyword";
    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "public class Super {",
            "   Object test() {",
            "       init(this.new NodeVisitor(), this.new EdgeVisitor());\n",
            "       return foo(this.new Bar(), this.new Foo(), getBuilder().new Foo());",
            "   }",
            "   Object foo(Bar b, Foo f) {",
            "     return Object();",
            "   }",
            "   class Foo{ }",
            "   class Bar{ }",
            "}")
        .expectOutput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable Object test() {",
            "       init(this.new NodeVisitor(), this.new EdgeVisitor());",
            "       return foo(this.new Bar(), this.new Foo(), getBuilder().new Foo());",
            "   }",
            "   Object foo(Bar b, Foo f) {",
            "     return Object();",
            "   }",
            "   class Foo{ }",
            "   class Bar{ }",
            "}")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "test()",
                "",
                "METHOD",
                "com.uber.Super",
                "Super.java",
                "true"))
        .start();
  }

  @Test
  public void skip_annotations_simple() {
    String rootName = "skip_annotations_simple";
    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   Object test(@javax.annotation.Nullable Object o) {",
            "   }",
            "}")
        .expectOutput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable Object test(@javax.annotation.Nullable Object o) {",
            "   }",
            "}")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "test(@javax.annotation.Nullable java.lang.Object)",
                "",
                "METHOD",
                "com.uber.Super",
                "Super.java",
                "true"))
        .start();
  }

  @Test
  public void remove_annot_return_nullable() {
    String rootName = "remove_annot_return_nullable";
    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable Object test(@javax.annotation.Nullable Object o) {",
            "   }",
            "}")
        .expectOutput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   Object test(@javax.annotation.Nullable Object o) {",
            "   }",
            "}")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "test(@javax.annotation.Nullable java.lang.Object)",
                "",
                "METHOD",
                "com.uber.Super",
                "Super.java",
                "false"))
        .start();
  }

  @Test
  public void remove_annot_param() {
    String rootName = "remove_annot_param";
    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable Object test(@Nullable Object o) {",
            "   }",
            "}")
        .expectOutput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable Object test(Object o) {",
            "   }",
            "}")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "test(@javax.annotation.Nullable java.lang.Object)",
                "o",
                "PARAMETER",
                "com.uber.Super",
                "Super.java",
                "false"))
        .start();
  }

  @Test
  public void remove_annot_param_full_name() {
    String rootName = "remove_annot_param_full_name";
    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable Object test(@javax.annotation.Nullable Object o) {",
            "   }",
            "}")
        .expectOutput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable Object test(Object o) {",
            "   }",
            "}")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "test(@javax.annotation.Nullable java.lang.Object)",
                "o",
                "PARAMETER",
                "com.uber.Super",
                "Super.java",
                "false"))
        .start();
  }

  @Test
  public void remove_annot_field() {
    String rootName = "remove_annot_field";
    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable Object f;",
            "   @Nullable Object test(@javax.annotation.Nullable Object o) {",
            "   }",
            "}")
        .expectOutput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   Object f;",
            "   @Nullable Object test(@javax.annotation.Nullable Object o) {",
            "   }",
            "}")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "",
                "f",
                "FIELD",
                "com.uber.Super",
                "Super.java",
                "false"))
        .start();
  }

  @Test
  public void simple_array_bracket_preservation() {
    String rootName = "remove_annot_field";
    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "A.java", "package com.uber;", "public class A {", "   private Object[] allTest;", "}")
        .expectOutput(
            "A.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class A {",
            "   @Nullable",
            "   private Object[] allTest;",
            "}")
        .addInput(
            "B.java", "package com.uber;", "public class B {", "   private Object allTest[];", "}")
        .expectOutput(
            "B.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class B {",
            "   @Nullable",
            "   private Object allTest[];",
            "}")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "",
                "allTest",
                "FIELD",
                "com.uber.B",
                "B.java",
                "true"),
            new Fix(
                "javax.annotation.Nullable",
                "",
                "allTest",
                "FIELD",
                "com.uber.A",
                "A.java",
                "true"))
        .start(true);
  }

  @Test
  public void custom_nullable_already_exists() {
    String rootName = "custom_nullable_already_exists";

    injectorTestHelper =
        new InjectorTestHelper()
            .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
            .addInput(
                "Main.java",
                "package com.uber;",
                "import custom.Nullable;",
                "public class Main {",
                "   public enum Test{",
                "     CLASSIC;",
                "     public Object run(){",
                "       return null;",
                "     }",
                "   }",
                "}")
            .expectOutput(
                "Main.java",
                "package com.uber;",
                "import custom.Nullable;",
                "public class Main {",
                "   public enum Test{",
                "     CLASSIC;",
                "     @Nullable",
                "     public Object run(){",
                "       return null;",
                "     }",
                "   }",
                "}")
            .addFixes(
                new Fix(
                    "javax.annotation.Nullable",
                    "run()",
                    "",
                    "METHOD",
                    "com.uber.Main$Test",
                    "Main.java",
                    "true"));
    injectorTestHelper.start();
  }
}
