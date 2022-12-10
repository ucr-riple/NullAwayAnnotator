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
import com.google.common.base.Preconditions;
import edu.ucr.cs.riple.injector.offsets.FileOffsetStore;
import java.util.List;

/** Represents a deletion of a content in the source file. */
public class Deletion extends Modification {

  /** The end position of content which should be removed. */
  private final Position endPosition;

  public Deletion(String content, Position startPosition, Position endPosition) {
    super(content, startPosition);
    // Position in javaparser is not 0 indexed and line and column fields are final.
    this.endPosition = new Position(endPosition.line - 1, endPosition.column - 1);
  }

  @Override
  public void visit(List<String> lines, FileOffsetStore offsetStore) {
    // all deletion logic below is written based on the fact that Injector only removes annotations
    // it added itself, therefore it does not cover all cases. All added annotations are inserted in
    // a single line and are followed by a space.
    Preconditions.checkArgument(
        startPosition.line == endPosition.line,
        "Cannot delete annotations that are written in multiple lines");
    StringBuilder line = new StringBuilder(lines.get(startPosition.line));
    // add extra 1 for the added white space.
    line.delete(startPosition.column, endPosition.column + 2);
    offsetStore.updateOffsetWithDeletion(
        startPosition.line, startPosition.column, endPosition.column - startPosition.column + 1);
    lines.set(startPosition.line, line.toString());
  }
}
