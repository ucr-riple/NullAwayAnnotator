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

/** Config of scanner. */
public interface Config {

  /**
   * If true, serialization services are activated.
   *
   * @return true, if activated, false otherwise.
   */
  boolean isActive();

  /**
   * Checks if the passed name is a {@code @Nonnull} annotation.
   *
   * @param annotName Given annotation name.
   * @return true, if the given name is name of an {@code @Nonnull} annotation.
   */
  boolean isNonnullAnnotation(String annotName);

  /**
   * Returns the using serializer.
   *
   * @return Using serializer.
   */
  Serializer getSerializer();

  /**
   * Returns root directory where all outputs will be serialized.
   *
   * @return Path to root directory where all outputs will be serialized.
   */
  @Nonnull
  Path getOutputDirectory();

  /**
   * Returns symbol resolver which can detect source type of elements.
   *
   * @return Using SymbolSourceResolver instance.
   */
  SymbolSourceResolver getSymbolSourceResolver();

  /**
   * Checks if the given symbol is an annotated package.
   *
   * @param symbol Given symbol.
   * @return true, if the given symbol is an annotated package.
   */
  boolean isAnnotatedPackage(Symbol symbol);
}
