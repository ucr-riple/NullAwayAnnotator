/*
 * MIT License
 *
 * Copyright (c) 2026 Nima Karimipour
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
 * Tests in this class are related to language levels released after Java 21. These tests include
 * blocks of code that are not syntactically supported by Java 21.
 */
public class Java25Test extends AnnotatorBaseCoreTest {

  public Java25Test() {
    super("java-25");
  }

  @Test
  public void moduleImportDeclarationTest() {
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Main.java",
            "package test;",
            "// module import declaration - finalized in Java 25 (JEP 511)",
            "import module java.base;",
            "public class Main {",
            "   Object f1, f2, f3, f4;",
            "   List<String> foo() {",
            "      return List.of();",
            "   }",
            "}")
        .withExpectedReports(
            new TReport(new OnField("Main.java", "test.Main", Set.of("f1", "f2", "f3", "f4")), -4))
        .withLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_25)
        .start();
  }

  @Test
  public void unnamedVariableTest() {
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Main.java",
            "package test;",
            "import java.util.List;",
            "public class Main {",
            "   Object f1, f2, f3, f4;",
            "   void foo(List<String> items) {",
            "      // unnamed variable - finalized in Java 22 (JEP 456)",
            "      for (String _ : items) { }",
            "   }",
            "}")
        .withExpectedReports(
            new TReport(new OnField("Main.java", "test.Main", Set.of("f1", "f2", "f3", "f4")), -4))
        .withLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_25)
        .start();
  }
}
