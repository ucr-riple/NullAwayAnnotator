package edu.ucr.cs.riple.injector;

import edu.ucr.cs.riple.injector.tools.InjectorTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ClassSearchTest {

  InjectorTestHelper injectorTestHelper;

  @Before
  public void setup() {}

  @Test
  public void class_search_declaration_in_method_body() {
    String rootName = "class_search_declaration_in_method_body";

    injectorTestHelper =
        new InjectorTestHelper()
            .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
            .addInput(
                "TargetMethodContextSelector.java",
                "package com.uber;",
                "public class TargetMethodContextSelector implements ContextSelector {",
                "   @Override",
                "   public Context getCalleeTarget() {",
                "     class MethodDispatchContext implements Context {",
                "        @Override",
                "        public ContextItem get(ContextKey name) { }",
                "     }",
                "   }",
                "}")
            .expectOutput(
                "TargetMethodContextSelector.java",
                "package com.uber;",
                "import javax.annotation.Nullable;",
                "public class TargetMethodContextSelector implements ContextSelector {",
                "   @Override",
                "   public Context getCalleeTarget() {",
                "     class MethodDispatchContext implements Context {",
                "        @Override @Nullable",
                "         public ContextItem get(ContextKey name) { }",
                "     }",
                "   }",
                "}")
            .addFixes(
                new Fix(
                    "javax.annotation.Nullable",
                    "get(com.ibm.wala.ipa.callgraph.ContextKey)",
                    "",
                    "METHOD",
                    "com.uber.TargetMethodContextSelector$1MethodDispatchContext",
                    "TargetMethodContextSelector.java",
                    "true"));
    injectorTestHelper.start();
  }

  @Test
  public void class_search_anonymous_1() {
    String rootName = "class_search_anonymous_1";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInputSourceFile("Main.java", "ClassSearch.java")
        .expectOutputFile("Main.java", "class_search_anonymous_1_expected.java")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "foo()",
                "",
                "METHOD",
                "injector.Main$1Helper$InnerHelper",
                "Main.java",
                "true"))
        .start(true);
  }
}
