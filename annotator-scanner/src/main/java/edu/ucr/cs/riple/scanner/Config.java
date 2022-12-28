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

import edu.ucr.cs.riple.scanner.generatedcode.SymbolSourceResolver;
import java.nio.file.Path;
import javax.annotation.Nonnull;

/** Config of scanner. */
public interface Config {

  /**
   * If true, all method calls invocations data will be serialized.
   *
   * @return true, if activated.
   */
  boolean callTrackerIsActive();

  /**
   * If true, all field usages data will be serialized.
   *
   * @return true, if activated.
   */
  boolean fieldTrackerIsActive();

  /**
   * If true, all methods information will be serialized.
   *
   * @return true, if activated.
   */
  boolean methodTrackerIsActive();

  /**
   * If true, all class data will be serialized.
   *
   * @return true, if activated.
   */
  boolean classTrackerIsActive();

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
}
