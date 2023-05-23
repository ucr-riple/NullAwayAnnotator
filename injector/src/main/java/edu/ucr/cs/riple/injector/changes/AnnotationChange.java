/*
 * Copyright (c) 2023 University of California, Riverside.
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

/** Marker interface for changes regarding annotations on elements in source code. */
public abstract class AnnotationChange implements ASTChange {

  /** Location of the element which its annotations should be changed. */
  public final Location location;
  /** Annotation name. */
  public final Name annotationName;

  public AnnotationChange(Location location, Name annotation) {
    this.location = location;
    this.annotationName = annotation;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AnnotationChange)) {
      return false;
    }
    AnnotationChange other = (AnnotationChange) o;
    return Objects.equals(location, other.location)
        && Objects.equals(annotationName, other.annotationName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(location, annotationName);
  }

  @Override
  public String toString() {
    return location.toString();
  }

  @Override
  public Location getLocation() {
    return location;
  }

  /**
   * Returns the fully qualified type name of the annotation as a {@link Name} instance.
   *
   * @return the name of the annotation.
   */
  public Name getAnnotationName() {
    return annotationName;
  }
}
