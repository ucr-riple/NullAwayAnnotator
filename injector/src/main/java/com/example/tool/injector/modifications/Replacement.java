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

package com.example.tool.injector.modifications;

import com.github.javaparser.Position;
import com.example.tool.injector.offsets.FileOffsetStore;
import java.util.List;

/** Represents a replacement of a content in the source file. */
public class Replacement extends Modification {

  /** The end position of the content that should be replaced. */
  private final Position endPosition;

  public Replacement(String content, Position startPosition, Position endPosition) {
    super(content, startPosition);
    // Position in javaparser is not 0 indexed and line and column fields are final.
    this.endPosition = new Position(endPosition.line - 1, endPosition.column - 1);
    if (content.equals("")) {
      throw new IllegalArgumentException("content cannot be empty, use Deletion instead");
    }
  }

  @Override
  public void visit(List<String> lines, FileOffsetStore offsetStore) {
    StringBuilder line = new StringBuilder(lines.get(startPosition.line));
    // Since replacements are only on fields and methods, we can compute paddings based on the
    // starting line.
    StringBuilder padding = new StringBuilder();
    int head = 0;
    while (head < line.length() && Character.isWhitespace(line.charAt(head))) {
      padding.append(line.charAt(head));
      head += 1;
    }
    head = startPosition.line;
    while (head < endPosition.line) {
      lines.remove(startPosition.line);
      head++;
    }
    line = new StringBuilder(lines.get(startPosition.line));
    line.delete(0, endPosition.column + 1);
    if (!line.toString().equals("")) {
      // keep other content if exists.
      lines.set(startPosition.line, padding + line.toString().trim());
    } else {
      lines.remove(startPosition.line);
    }
    lines.add(startPosition.line, padding + content);
  }
}
