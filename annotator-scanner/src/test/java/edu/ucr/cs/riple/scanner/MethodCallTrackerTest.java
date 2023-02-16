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

package edu.ucr.cs.riple.scanner;

import com.google.common.base.Preconditions;
import edu.ucr.cs.riple.scanner.tools.DisplayFactory;
import edu.ucr.cs.riple.scanner.tools.TrackerNodeDisplay;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MethodCallTrackerTest extends AnnotatorScannerBaseTest<TrackerNodeDisplay> {

  private static final DisplayFactory<TrackerNodeDisplay> METHOD_TRACKER_DISPLAY_FACTORY =
      values -> {
        Preconditions.checkArgument(values.length == 5, "Expected to find 5 values on each line");
        return new TrackerNodeDisplay(values[0], values[1], values[3], values[2], values[4]);
      };
  private static final String HEADER =
      "REGION_CLASS"
          + '\t'
          + "REGION_MEMBER"
          + '\t'
          + "USED_MEMBER"
          + '\t'
          + "USED_CLASS"
          + '\t'
          + "SOURCE_TYPE";
  private static final String FILE_NAME = "call_graph.tsv";

  public MethodCallTrackerTest() {
    super(METHOD_TRACKER_DISPLAY_FACTORY, HEADER, FILE_NAME);
  }

  @Test
  public void BasicTest() {
    tester
        .addSourceLines(
            "edu/ucr/A.java",
            "package edu.ucr;",
            "public class A {",
            "   public Object bar(){",
            "      Other o = new Other();",
            "      return o.foo();",
            "   }",
            "}",
            "class Other {",
            "   Object foo() { return null; };",
            "}")
        .setExpectedOutputs(new TrackerNodeDisplay("edu.ucr.A", "bar()", "edu.ucr.Other", "foo()"))
        .doTest();
  }

  @Test
  public void callableTests() {
    tester
        .addSourceLines(
            "edu/ucr/A.java",
            "package edu.ucr;",
            "public class A {",
            "   A a = new A();",
            "   A b = new A(0);",
            "   A() { }",
            "   A(int i) { }",
            "   void bar() {",
            "       A a = new A();",
            "   }",
            "}")
        .setExpectedOutputs(
            new TrackerNodeDisplay("edu.ucr.A", "a", "edu.ucr.A", "A()"),
            new TrackerNodeDisplay("edu.ucr.A", "b", "edu.ucr.A", "A(int)"),
            new TrackerNodeDisplay("edu.ucr.A", "bar()", "edu.ucr.A", "A()"),
            new TrackerNodeDisplay("edu.ucr.A", "A()", "java.lang.Object", "Object()"),
            new TrackerNodeDisplay("edu.ucr.A", "A(int)", "java.lang.Object", "Object()"))
        .doTest();
  }

  @Test
  public void fieldDeclaredRegionComputationAllCasesCallables() {
    tester
        .addSourceLines(
            "edu/ucr/A.java",
            "package edu.ucr;",
            "public class A {",
            "   B b = new B();",
            "   Object f0 = b.get();",
            "   Object f1 = B.staticB();",
            "   Object f2 = b.c.get();",
            "   Object f3 = B.staticC.get();",
            "   {",
            "       B.staticB();",
            "   }",
            "}",
            "class B {",
            "   C c = new C();",
            "   static C staticC = new C();",
            "   Object get() {",
            "       return new Object();",
            "   }",
            "   static Object staticB() {",
            "       return new Object();",
            "   }",
            "}",
            "class C {",
            "   Object val;",
            "   static Object get() {",
            "       return new Object();",
            "   }",
            "}")
        .setExpectedOutputs(
            new TrackerNodeDisplay("edu.ucr.A", "f0", "edu.ucr.B", "get()"),
            new TrackerNodeDisplay("edu.ucr.A", "f1", "edu.ucr.B", "staticB()"),
            new TrackerNodeDisplay("edu.ucr.A", "f2", "edu.ucr.C", "get()"),
            new TrackerNodeDisplay("edu.ucr.A", "f3", "edu.ucr.C", "get()"),
            new TrackerNodeDisplay("edu.ucr.A", "null", "edu.ucr.B", "staticB()"))
        .doTest();
  }
}
