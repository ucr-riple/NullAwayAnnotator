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
import java.util.Objects;
import javax.lang.model.element.ElementKind;

/**
 * Represents a text modification which are translation of {@link
 * edu.ucr.cs.riple.injector.changes.Change} instances.
 */
public abstract class Modification {

  /** Starting position where the modification should be applied in the source file. */
  public final Position startPosition;
  /** Content of modification. */
  public final String content;
  /** {@link ElementKind} of the element where this modification is created for. */
  public final ElementKind kind;

  public Modification(String content, Position startPosition, ElementKind kind) {
    // Position in javaparser is not 0 indexed and line and column fields are final.
    this.startPosition = new Position(startPosition.line - 1, startPosition.column - 1);
    this.content = content;
    this.kind = kind;
  }

  /**
   * Visits the source file as list of lines and applies its modification to it.
   *
   * @param lines List of lines of the target source code.
   */
  public abstract void visit(List<String> lines);

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Modification)) {
      return false;
    }
    Modification that = (Modification) o;
    return startPosition.equals(that.startPosition)
        && content.equals(that.content)
        && kind == that.kind;
  }

  @Override
  public int hashCode() {
    return Objects.hash(startPosition, content, kind);
  }
}
