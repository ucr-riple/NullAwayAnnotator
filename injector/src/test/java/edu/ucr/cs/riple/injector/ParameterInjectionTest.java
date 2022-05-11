package edu.ucr.cs.riple.injector;

import edu.ucr.cs.riple.injector.tools.InjectorTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ParameterInjectionTest {
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
        .addLocations(
            new Change(
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
        .addLocations(
            new Change(
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
        .addLocations(
            new Change(
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
        .addLocations(
            new Change(
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
        .addLocations(
            new Change(
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
        .addLocations(
            new Change(
                "javax.annotation.Nullable",
                "WeakKeyReference(@org.checkerframework.checker.nullness.qual.Nullable K,java.lang.ref.ReferenceQueue<K>)",
                "queue",
                "PARAMETER",
                "com.uber.WeakKeyReference",
                "WeakKeyReference.java",
                "true"))
        .start();
  }
}
