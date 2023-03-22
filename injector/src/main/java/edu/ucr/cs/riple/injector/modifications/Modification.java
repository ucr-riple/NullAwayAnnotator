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

package edu.ucr.cs.riple.injector.modifications;

import edu.ucr.cs.riple.injector.offsets.FileOffsetStore;
import java.util.List;

/**
 * Represents a text modification in the source file which are translation of {@link
 * edu.ucr.cs.riple.injector.changes.Change} instances.
 */
public interface Modification extends Comparable<Modification> {

  /**
   * Visits the source file as list of lines and applies its modification to it.
   *
   * @param lines List of lines of the target source code.
   * @param offsetStore Offset change info of the original version.
   */
  void visit(List<String> lines, FileOffsetStore offsetStore);

  /**
   * Returns the line number of the beginning of the modification on the source file. Required to
   * sort a collection of modifications.
   *
   * @return Tine line number of the beginning of the modification on the source file.
   */
  int getStartLine();

  /**
   * Returns the column number of the beginning of the modification on the source file. Required to
   * sort a collection of modifications.
   *
   * @return Tine column number of the beginning of the modification on the source file.
   */
  int getStarColumn();
}
