/*
 * Copyright (c) 2022 University of California, Riverside.
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

public class Deletion extends Modification {

  private final Position endPosition;

  public Deletion(String content, Position startPosition, Position endPosition, ElementKind kind) {
    super(content, startPosition, kind);
    this.endPosition = endPosition;
  }

  @Override
  public void visit(List<String> lines, Offset offset) {
    String line = lines.get(offset.line + startPosition.line);
    String removed =
        line.substring(0, offset.column + startPosition.column)
            + line.substring(offset.column + startPosition.column + content.length() + 1);

    if (removed.strip().equals("")) {
      lines.remove(offset.line + startPosition.line);
      offset.line -= 1;
      return;
    }
    lines.set(offset.line + startPosition.line, removed);
    offset.column -= endPosition.column - startPosition.column;
  }
}
