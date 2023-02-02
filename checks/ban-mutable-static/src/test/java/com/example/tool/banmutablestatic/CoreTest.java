/*
 * MIT License
 *
 * Copyright (c) 2022 anonymous
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

package com.example.tool.banmutablestatic;

import com.google.errorprone.CompilationTestHelper;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CoreTest {
  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  protected CompilationTestHelper defaultCompilationHelper;

  @SuppressWarnings("CheckReturnValue")
  @Before
  public void setup() {
    defaultCompilationHelper =
        CompilationTestHelper.newInstance(BanMutableStatic.class, getClass())
            .setArgs(Arrays.asList("-d", temporaryFolder.getRoot().getAbsolutePath()));
  }

  @Test
  public void testMutableStaticField() {
    defaultCompilationHelper
        .addSourceLines(
            "Main.java",
            "public class Main {",
            "    // BUG: Diagnostic contains: Variable field is declared as a mutable static field",
            "    static Object field;",
            "}")
        .doTest();
  }

  @Test
  public void testImmutableStaticField() {
    defaultCompilationHelper
        .addSourceLines(
            "Main.java",
            "public class Main {",
            "    final static Object field = new Object();",
            "}")
        .doTest();
  }

  @Test
  public void testOtherFields() {
    defaultCompilationHelper
        .addSourceLines(
            "Main.java",
            "public class Main {",
            "    final Object foo = new Object();",
            "    Object bar = new Object();",
            "}")
        .doTest();
  }

  @Test
  public void testMethod() {
    defaultCompilationHelper
        .addSourceLines(
            "Main.java",
            "public class Main {",
            "    final void foo() { }",
            "    final static void bar() { }",
            "    void fooBar() { }",
            "}")
        .doTest();
  }
}
