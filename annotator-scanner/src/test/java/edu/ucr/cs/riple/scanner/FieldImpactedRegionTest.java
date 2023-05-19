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
import edu.ucr.cs.riple.scanner.tools.ImpactedRegionRecordDisplay;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FieldImpactedRegionTest extends AnnotatorScannerBaseTest<ImpactedRegionRecordDisplay> {

  private static final DisplayFactory<ImpactedRegionRecordDisplay> FIELD_TRACKER_DISPLAY_FACTORY =
      values -> {
        Preconditions.checkArgument(values.length == 5, "Expected to find 5 values on each line");
        return new ImpactedRegionRecordDisplay(
            values[0], values[1], values[3], values[2], values[4]);
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
  private static final String FILE_NAME = "field_impacted_region_map.tsv";

  public FieldImpactedRegionTest() {
    super(FIELD_TRACKER_DISPLAY_FACTORY, HEADER, FILE_NAME);
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
            "      return o.foo;",
            "   }",
            "}",
            "class Other {",
            "   Object foo;",
            "}")
        .setExpectedOutputs(
            new ImpactedRegionRecordDisplay("edu.ucr.A", "bar()", "edu.ucr.Other", "foo"))
        .doTest();
  }

  @Test
  public void fieldDeclaredInInnerClassInMethod() {
    tester
        .addSourceLines(
            "edu/ucr/A.java",
            "package edu.ucr;",
            "public class A {",
            "   Other other = new Other();",
            "   public void bar(){",
            "      class Foo {;",
            "          Object baz = other.foo;",
            "      }",
            "   }",
            "}",
            "class Other {",
            "   Object foo;",
            "}")
        .setExpectedOutputs(
            new ImpactedRegionRecordDisplay("edu.ucr.A$1Foo", "baz", "edu.ucr.Other", "foo"),
            new ImpactedRegionRecordDisplay("edu.ucr.A$1Foo", "baz", "edu.ucr.Other", "foo"),
            new ImpactedRegionRecordDisplay("edu.ucr.A$1Foo", "baz", "edu.ucr.A", "other"))
        .doTest();
  }

  @Test
  public void fieldDeclaredRegionComputationAllCases() {
    tester
        .addSourceLines(
            "edu/ucr/A.java",
            "package edu.ucr;",
            "public class A {",
            "   B b = new B();",
            "   Object f0 = b.foo;",
            "   Object f1 = B.staticFoo;",
            "   Object f2 = B.bar();",
            "   Object f3 = b.c.val;",
            "   Object f4 = B.staticC.val;",
            "}",
            "class B {",
            "   C c = new C();",
            "   static C staticC = new C();",
            "   Object foo;",
            "   static Object staticFoo;",
            "   static Object bar() {",
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
            new ImpactedRegionRecordDisplay("edu.ucr.A", "f0", "edu.ucr.B", "foo"),
            new ImpactedRegionRecordDisplay("edu.ucr.A", "f0", "edu.ucr.B", "foo"),
            new ImpactedRegionRecordDisplay("edu.ucr.A", "b", "edu.ucr.A", "b"),
            new ImpactedRegionRecordDisplay("edu.ucr.A", "f1", "edu.ucr.B", "staticFoo"),
            new ImpactedRegionRecordDisplay("edu.ucr.A", "f1", "edu.ucr.B", "staticFoo"),
            new ImpactedRegionRecordDisplay("edu.ucr.A", "f3", "edu.ucr.C", "val"),
            new ImpactedRegionRecordDisplay("edu.ucr.A", "f3", "edu.ucr.C", "val"),
            new ImpactedRegionRecordDisplay("edu.ucr.A", "f3", "edu.ucr.B", "c"),
            new ImpactedRegionRecordDisplay("edu.ucr.A", "b", "edu.ucr.A", "b"),
            new ImpactedRegionRecordDisplay("edu.ucr.A", "f4", "edu.ucr.C", "val"),
            new ImpactedRegionRecordDisplay("edu.ucr.A", "f4", "edu.ucr.C", "val"),
            new ImpactedRegionRecordDisplay("edu.ucr.A", "f4", "edu.ucr.B", "staticC"))
        .doTest();
  }

  @Test
  public void LombokGeneratedCodeDetectionTest() {
    tester
        .addSourceLines(
            "lombok/Generated.java", "package lombok;", "public @interface Generated { }")
        .addSourceLines(
            "A.java",
            "import lombok.Generated;",
            "public class A {",
            "   Object foo;",
            "   @lombok.Generated()",
            "   public Object bar(){",
            "       return foo;",
            "   }",
            "}")
        .setExpectedOutputs(new ImpactedRegionRecordDisplay("A", "bar()", "A", "foo", "LOMBOK"))
        .doTest();
  }
}
