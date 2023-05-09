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

package edu.ucr.cs.riple.injector.changes;

import edu.ucr.cs.riple.injector.location.Location;
import java.util.Objects;

/** Used to add annotations on elements in source code. */
public abstract class AddAnnotation extends ASTChange {

  public AddAnnotation(Location location, String annotation) {
    super(location, annotation);
  }

  @Override
  public boolean equals(Object other) {
    boolean superAns = super.equals(other);
    if (!superAns) {
      return false;
    }
    return other instanceof AddAnnotation;
  }

  @Override
  public int hashCode() {
    return Objects.hash("Add", super.hashCode());
  }

  /**
   * Returns the reverse change of this change. Can be used to undo this change by making a
   * corresponding {@link RemoveAnnotation} change.
   *
   * @return The reverse change of this annotation injection.
   */
  public abstract RemoveAnnotation getReverse();
}
