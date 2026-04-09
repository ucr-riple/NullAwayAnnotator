/*
 * MIT License
 *
 * Copyright (c) 2024 Nima Karimipour
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

import com.github.javaparser.ParserConfiguration;
import edu.ucr.cs.riple.core.tools.TReport;
import edu.ucr.cs.riple.injector.location.OnField;
import java.util.Set;
import org.junit.Test;

/**
 * Tests in this class are related to Java 21 features. These tests include blocks of code that are
 * not syntactically supported by Java 17.
 */
public class Java21Test extends AnnotatorBaseCoreTest {

  public Java21Test() {
    super("java-21");
  }

  @Test
  public void recordPatternInInstanceofTest() {
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Main.java",
            "package test;",
            "public class Main {",
            "   record Point(double x, double y) {}",
            "   Object f1, f2, f3, f4;",
            "   void foo() {",
            "      // record pattern in instanceof - finalized in Java 21 (JEP 440)",
            "      if (f1 instanceof Point(double x, double y)) { }",
            "   }",
            "}")
        .withExpectedReports(
            new TReport(new OnField("Main.java", "test.Main", Set.of("f1", "f2", "f3", "f4")), -4))
        .withLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
        .start();
  }
}
