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

import edu.ucr.cs.riple.scanner.tools.DisplayFactory;
import edu.ucr.cs.riple.scanner.tools.EffectiveMethodRecordDisplay;
import org.junit.Test;

public class EffectiveMethodRecordTest
    extends AnnotatorScannerBaseTest<EffectiveMethodRecordDisplay> {

  private static final String HEADER = "CLASS\tMETHOD\tPARAMETER";
  private static final String FILE_NAME = "effective_method_records.tsv";
  private static final DisplayFactory<EffectiveMethodRecordDisplay> FACTORY =
      values -> new EffectiveMethodRecordDisplay(values[0], values[1], values[2]);

  public EffectiveMethodRecordTest() {
    super(FACTORY, HEADER, FILE_NAME);
  }

  @Test
  public void simpleTest() {
    tester
        .addSourceLines(
            "edu/ucr/IFace.java",
            "package edu.ucr;",
            "public interface IFace {",
            "  void foo(Object p1);",
            "}")
        .addSourceLines(
            "edu/ucr/A.java",
            "package edu.ucr;",
            "public class A {",
            "  void foo1(Object p1, final Object p2){",
            "    useIFace((p3) -> System.out.println(p3));",
            "    class C {",
            "      Object p1;",
            "      void t1(Object p1){",
            "         p1.toString();",
            "      }",
            "      void t2(){",
            "         p1.toString();",
            "      }",
            "    }",
            "    bar(p2);",
            "  }",
            "  void foo2(Object p1, Object p2){",
            "    foo1(p1, p1);",
            "    foo1(p1, p2);",
            "    switch(p1.toString()){",
            "      case \"a\":",
            "        break;",
            "      default:",
            "        break;",
            "    }",
            "  }",
            "  void bar(Object p){",
            "    System.out.println(p);",
            "  }",
            "  void useIFace(IFace iface){}",
            "}")
        .setExpectedOutputs(
            new EffectiveMethodRecordDisplay("edu.ucr.A$1C", "t1(java.lang.Object)", "p1"),
            new EffectiveMethodRecordDisplay(
                "edu.ucr.A", "foo2(java.lang.Object,java.lang.Object)", "p1"))
        .doTest();
  }
}
