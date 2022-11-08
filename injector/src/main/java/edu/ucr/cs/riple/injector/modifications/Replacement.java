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
import java.util.List;
import javax.lang.model.element.ElementKind;

/** Represents a replacement of a content in the source file. */
public class Replacement extends Modification {

  /** The end position of the content that should be replaced. */
  private final Position endPosition;

  public Replacement(
      String content, Position startPosition, Position endPosition, ElementKind kind) {
    super(content, startPosition, kind);
    // Position in javaparser is not 0 indexed and line and column fields are final.
    this.endPosition = new Position(endPosition.line - 1, endPosition.column - 1);
    if (content.equals("")) {
      throw new IllegalArgumentException("content cannot be empty, use Deletion instead");
    }
  }

  @Override
  public void visit(List<String> lines) {
    StringBuilder line = new StringBuilder(lines.get(startPosition.line));
    line.replace(startPosition.column, endPosition.column + 1, content);
    lines.set(startPosition.line, line.toString());
  }
}
