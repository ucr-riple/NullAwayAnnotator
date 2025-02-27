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

import com.sun.tools.javac.code.Symbol;
import edu.ucr.cs.riple.scanner.generatedcode.SymbolSourceResolver;
import java.nio.file.Path;
import javax.annotation.Nonnull;

/**
 * Empty config just to stop the process if an error is occurred in configuring the Scanner via
 * Error Prone flags.
 */
public class DummyOptionsConfig implements Config {

  static final String ERROR_MESSAGE =
      "Error in configuring Scanner Checker, a path must be passed via Error Prone Flag (-XepOpt:AnnotatorScanner:ConfigPath) where output directory is in XML format.";

  @Override
  public boolean isActive() {
    throw new IllegalStateException(ERROR_MESSAGE);
  }

  @Override
  public boolean isNonnullAnnotation(String annotName) {
    throw new IllegalStateException(ERROR_MESSAGE);
  }

  @Override
  public Serializer getSerializer() {
    throw new IllegalStateException(ERROR_MESSAGE);
  }

  @Nonnull
  @Override
  public Path getOutputDirectory() {
    throw new IllegalStateException(ERROR_MESSAGE);
  }

  @Override
  public SymbolSourceResolver getSymbolSourceResolver() {
    throw new IllegalStateException(ERROR_MESSAGE);
  }

  @Override
  public boolean isAnnotatedPackage(Symbol symbol) {
    throw new IllegalStateException(ERROR_MESSAGE);
  }
}
