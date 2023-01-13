/*
 * MIT License
 *
 * Copyright (c) 2023 Nima Karimipour
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
import edu.ucr.cs.riple.scanner.location.SymbolLocation;
import edu.ucr.cs.riple.scanner.tools.DisplayFactory;
import edu.ucr.cs.riple.scanner.tools.LocationDisplay;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class NonnullSerializationTest extends AnnotatorScannerBaseTest<LocationDisplay> {

  private static final DisplayFactory<LocationDisplay> LOCATION_DISPLAY_FACTORY =
      values -> {
        Preconditions.checkArgument(values.length == 6, "Expected to find 6 values on each line");
        return new LocationDisplay(
            values[0], values[1], values[2], values[3], values[4], values[5]);
      };
  private static final String HEADER = SymbolLocation.header();
  private static final String FILE_NAME = Serializer.NON_NULL_ELEMENTS_FILE_NAME;

  public NonnullSerializationTest() {
    super(LOCATION_DISPLAY_FACTORY, HEADER, FILE_NAME);
  }

  @Test
  public void BasicTest() {
    tester
        .addSourceLines(
            "edu/ucr/Main.java",
            "package test;",
            "import javax.annotation.Nonnull;",
            "public class Main {",
            "   @Nonnull Object field;",
            "   @Nonnull",
            "   Object foo() {",
            "       bar(null, null);",
            "       return null;",
            "   }",
            "   Object bar(Object p1, @Nonnull Object p2) {",
            "       return null;",
            "   }",
            "}")
        .setExpectedOutputs(
            new LocationDisplay("FIELD", "test.Main", "null", "field", "0", "/edu/ucr/Main.java"),
            new LocationDisplay(
                "METHOD", "test.Main", "foo()", "null", "null", "/edu/ucr/Main.java"),
            new LocationDisplay(
                "PARAMETER",
                "test.Main",
                "bar(java.lang.Object,java.lang.Object)",
                "p2",
                "1",
                "/edu/ucr/Main.java"))
        .doTest();
  }
}
