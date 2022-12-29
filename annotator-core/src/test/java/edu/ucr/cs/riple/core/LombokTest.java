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

package edu.ucr.cs.riple.core;

import edu.ucr.cs.riple.core.tools.TReport;
import edu.ucr.cs.riple.injector.location.OnField;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class LombokTest extends BaseCoreTest {

  public LombokTest() {
    super("lombok", List.of("unittest"));
  }

  @Test
  public void regionComputationOnFieldParallelProcessingEnables() {
    coreTestHelper
        .addInputLines(
            "Main.java",
            "package test;",
            "import lombok.Data;",
            "@Data",
            "class Main {",
            "   Object f;",
            "   public Object m1(){",
            "       return getF();", // Should be error
            "   }",
            "   public Object m2(){",
            "       return getF();", // Should be error
            "   }",
            "}")
        .toDepth(1)
        .addExpectedReports(
            new TReport(new OnField("Main.java", "test.Main", Collections.singleton("f")), 1))
        .start();
  }
}
