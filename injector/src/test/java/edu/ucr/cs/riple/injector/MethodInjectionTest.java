package edu.ucr.cs.riple.injector;

import edu.ucr.cs.riple.injector.tools.InjectorTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MethodInjectionTest {

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
        .addLocations(
            new Location(
                "javax.annotation.Nullable",
                "test(boolean)",
                "",
                "METHOD",
                "com.uber.Super",
                "Super.java",
                "true"),
            new Location(
                "javax.annotation.Nullable",
                "test(boolean)",
                "",
                "METHOD",
                "com.uber.Superb",
                "com/Superb.java",
                "true"))
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
        .addLocations(
            new Location(
                "javax.annotation.Nullable",
                "run()",
                "",
                "METHOD",
                "com.uber.Main$Test",
                "Main.java",
                "true"))
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
        .addLocations(
            new Location(
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
        .addLocations(
            new Location(
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
        .addLocations(
            new Location(
                "javax.annotation.Nullable",
                "test(Object, String, String)",
                "",
                "METHOD",
                "com.uber.Super",
                "Super.java",
                "true"),
            new Location(
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
        .addLocations(
            new Location(
                "javax.annotation.Nullable",
                "getPredNodeNumbers(T)",
                "",
                "METHOD",
                "com.uber.Super",
                "Super.java",
                "true"),
            new Location(
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
        .addLocations(
            new Location(
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
        .addLocations(
            new Location(
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
        .addLocations(
            new Location(
                "javax.annotation.Initializer",
                "format(java.lang.String,java.lang.Object...)",
                "",
                "METHOD",
                "com.uber.Main",
                "Main.java",
                "true"))
        .start();
  }
}
