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

import com.github.javaparser.Position;
import edu.ucr.cs.riple.injector.offsets.FileOffsetStore;
import java.util.List;

/** Represents an insertion of a content in the source file. */
public class Insertion extends Modification {

  public Insertion(String content, Position position) {
    super(content, position);
  }

  @Override
  public void visit(List<String> lines, FileOffsetStore offsetStore) {
    String toAdd = this.content + " ";
    String line = lines.get(startPosition.line);
    offsetStore.updateOffsetWithAddition(startPosition.line, startPosition.column, toAdd.length());
    lines.set(
        startPosition.line,
        line.substring(0, startPosition.column) + toAdd + line.substring(startPosition.column));
  }
}
