package edu.ucr.cs.riple.injector;

import edu.ucr.cs.riple.injector.location.OnMethod;
import edu.ucr.cs.riple.injector.location.OnParameter;
import edu.ucr.cs.riple.injector.tools.InjectorTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class OnMethodInjectionTest {

  @Test
  public void method_nullable_simple() {
    String rootName = "method_nullable_simple";
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
        .addChanges(
            new Change(
                new OnMethod("Super.java", "com.uber.Super", "test(boolean)"),
                "javax.annotation.Nullable",
                true),
            new Change(
                new OnMethod("com/Superb.java", "com.uber.Superb", "test(boolean)"),
                "javax.annotation.Nullable",
                true))
        .start();
  }

  @Test
  public void method_nullable_enum_simple() {
    String rootName = "method_nullable_enum_simple";
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
        .addChanges(
            new Change(
                new OnMethod("Main.java", "com.uber.Main$Test", "run()"),
                "javax.annotation.Nullable",
                true))
        .start();
  }

  @Test
  public void method_nullable_inner_class() {
    String rootName = "method_nullable_inner_class";

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
        .addChanges(
            new Change(
                new OnMethod("Super.java", "com.uber.Super$SuperInner", "bar(java.lang.Object)"),
                "javax.annotation.Nullable",
                true))
        .start();
  }

  @Test
  public void method_nullable_class_decl_in_method() {
    String rootName = "method_nullable_class_decl_in_method";
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
        .addChanges(
            new Change(
                new OnMethod("Main.java", "com.uber.Main$1Helper", "run()"),
                "javax.annotation.Nullable",
                true))
        .start();
  }

  @Test
  public void method_nullable_signature_duplicate_type() {
    String rootName = "method_nullable_signature_duplicate_type";

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
        .addChanges(
            new Change(
                new OnMethod("Super.java", "com.uber.Super", "test(Object, String, String)"),
                "javax.annotation.Nullable",
                true),
            new Change(
                new OnParameter(
                    "Super.java", "com.uber.Super", "test(Object, Object, String)", "name", 1),
                "javax.annotation.Nullable",
                true))
        .start();
  }

  @Test
  public void method_nullable_single_generic_method_pick() {
    String rootName = "method_nullable_single_generic_method_pick";

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
        .addChanges(
            new Change(
                new OnMethod("Super.java", "com.uber.Super", "getPredNodeNumbers(T)"),
                "javax.annotation.Nullable",
                true),
            new Change(
                new OnMethod(
                    "Super.java",
                    "com.uber.Super",
                    "computeResult(com.ibm.wala.ipa.slicer.Statement,java.util.Map<com.ibm.wala.ipa.callgraph.propagation.PointerKey,com.ibm.wala.util.intset.MutableIntSet>,com.ibm.wala.dataflow.graph.BitVectorSolver<? extends com.ibm.wala.ssa.ISSABasicBlock>,com.ibm.wala.util.intset.OrdinalSetMapping<com.ibm.wala.ipa.slicer.Statement>,com.ibm.wala.ipa.callgraph.CGNode,com.ibm.wala.ipa.modref.ExtendedHeapModel,com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis<T>,java.util.Map<com.ibm.wala.ipa.callgraph.CGNode,com.ibm.wala.util.intset.OrdinalSet<com.ibm.wala.ipa.callgraph.propagation.PointerKey>>,com.ibm.wala.ssa.analysis.ExplodedControlFlowGraph,java.util.Map<java.lang.Integer,com.ibm.wala.ipa.slicer.NormalStatement>)"),
                "javax.annotation.Nullable",
                true))
        .start();
  }

  @Test
  public void method_nullable_signature_array_brackets() {
    String rootName = "method_nullable_signature_array_brackets";

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
        .addChanges(
            new Change(
                new OnMethod(
                    "Super.java",
                    "com.uber.Super",
                    "getTargetForCall(com.ibm.wala.ipa.callgraph.CGNode[],com.ibm.wala.classLoader.CallSiteReference[][][],com.ibm.wala.classLoader.IClass,com.ibm.wala.ipa.callgraph.propagation.InstanceKey[][])"),
                "javax.annotation.Nullable",
                true))
        .start(true);
  }

  @Test
  public void method_nullable_signature_generic_method_name() {
    String rootName = "method_nullable_signature_generic_method_name";

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
        .addChanges(
            new Change(
                new OnMethod(
                    "Super.java",
                    "com.uber.Super",
                    "<T>getReader(com.ibm.wala.shrikeCT.ClassReader.AttrIterator,java.lang.String,com.ibm.wala.classLoader.ShrikeClass.GetReader<T>)"),
                "javax.annotation.Nullable",
                true))
        .start();
  }

  @Test
  public void method_nullable_dot_array() {
    String rootName = "method_nullable_dot_array";

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
        .addChanges(
            new Change(
                new OnMethod(
                    "Main.java", "com.uber.Main", "format(java.lang.String,java.lang.Object...)"),
                "javax.annotation.Initializer",
                true))
        .start();
  }
}
