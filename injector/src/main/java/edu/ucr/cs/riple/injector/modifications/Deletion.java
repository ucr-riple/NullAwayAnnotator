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

/** Represents a deletion of a content in the source file. */
public class Deletion extends Modification {

  /** The end position of content which should be removed. */
  private final Position endPosition;

  public Deletion(String content, Position startPosition, Position endPosition, ElementKind kind) {
    super(content, startPosition, kind);
    this.endPosition = new Position(endPosition.line - 1, endPosition.column - 1);
  }

  @Override
  public void visit(List<String> lines) {
    int start = startPosition.column;
    int end = endPosition.column;
    StringBuilder line = new StringBuilder(lines.get(startPosition.line));
    line.delete(start, end + 1);
    // char at start is removed, head is one character before start.
    int head = start - 1;
    // if head is surrounded by white space, remove the extra whitespace.
    // (  ...) -> ( ...)
    if (head > 1 && line.charAt(head - 1) == ' ' && line.charAt(head) == ' ') {
      line.deleteCharAt(head);
    }
    // if head is not alphabetic and is followed by a whitespace, remove it.
    // (@Nullable ...) -> (...)
    if (head + 1 < line.length()
        && !Character.isLetterOrDigit(line.charAt(head))
        && line.charAt(head + 1) == ' ') {
      // remove extra white space
      line.deleteCharAt(head + 1);
    }
    String removed = line.toString();
    if (removed.strip().equals("")) {
      lines.remove(startPosition.line);
      return;
    }
    lines.set(startPosition.line, removed);
  }
}
