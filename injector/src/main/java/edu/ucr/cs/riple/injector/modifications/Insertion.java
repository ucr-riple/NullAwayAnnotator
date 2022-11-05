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

/** Represents an insertion of a content in the source file. */
public class Insertion extends Modification {

  public Insertion(String content, Position position, ElementKind kind) {
    super(content, position, kind);
  }

  @Override
  public void visit(List<String> lines) {
    String toInsert;
    String line = lines.get(startPosition.line);
    switch (kind) {
      case METHOD:
      case FIELD:
        // need to compute padding manually, cannot use position.column as tab and space
        // both reserve one column.
        StringBuilder padding = new StringBuilder();
        int head = 0;
        while (head < line.length() && Character.isWhitespace(line.charAt(head))) {
          padding.append(line.charAt(head));
          head += 1;
        }
        toInsert = padding + this.content;
        lines.add(startPosition.line, toInsert);
        break;
      case PARAMETER:
        toInsert = this.content + " ";
        lines.set(
            startPosition.line,
            line.substring(0, startPosition.column)
                + toInsert
                + line.substring(startPosition.column));
        break;
      default:
        throw new IllegalArgumentException("Modification on kind: " + kind + " is not supported");
    }
  }
}
