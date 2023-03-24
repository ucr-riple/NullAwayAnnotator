/*
 * MIT License
 *
 * Copyright (c) 2023 Nima Karimipour
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
import java.util.Objects;

/**
 * Base class for modifications which require only a single string modification at a position in the
 * source file.
 */
public abstract class SinglePositionModification implements Modification {

  /** Starting position where the modification should be applied in the source file. */
  public final Position startPosition;
  /** Content of modification. */
  public final String content;

  public SinglePositionModification(String content, Position startPosition) {
    // Position in javaparser is not 0 indexed and line and column fields are final.
    this.startPosition = new Position(startPosition.line - 1, startPosition.column - 1);
    this.content = content;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SinglePositionModification)) {
      return false;
    }
    SinglePositionModification that = (SinglePositionModification) o;
    return startPosition.equals(that.startPosition) && content.equals(that.content);
  }

  @Override
  public int hashCode() {
    return Objects.hash(startPosition, content);
  }

  @Override
  public Position getStartingPosition() {
    return startPosition;
  }

  @Override
  public int compareTo(Modification o) {
    int value = COMPARATOR.compare(this, o);
    if (value != 0) {
      return value;
    }
    if (o instanceof SinglePositionModification) {
      // to produce deterministic results in unit tests and avoid flaky CI issues. In practice, the
      // order is not important anymore and will not cause any problem.
      return content.compareTo(((SinglePositionModification) o).content);
    }
    return 0;
  }
}
