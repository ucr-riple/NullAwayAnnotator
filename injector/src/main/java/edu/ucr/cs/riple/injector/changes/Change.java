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

import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.NodeWithRange;
import edu.ucr.cs.riple.injector.Helper;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.LocationToJsonVisitor;
import edu.ucr.cs.riple.injector.modifications.Modification;
import java.util.Objects;
import javax.annotation.Nullable;
import org.json.simple.JSONObject;

/** Represents a change in the AST of the source code. */
public abstract class Change {
  /** Target location. */
  public final Location location;
  /** Annotation full name. */
  public final String annotation;
  /** Annotation simple name. */
  public final String annotationSimpleName;

  public Change(Location location, String annotation) {
    this.annotation = annotation;
    this.location = location;
    this.annotationSimpleName = Helper.simpleName(annotation);
  }

  /**
   * Visits the given node and translates the change to a text modification.
   *
   * @param node Given node.
   * @return A text modification instance if the translation is successful, otherwise {@code null}
   *     will be returned.
   */
  @Nullable
  public abstract <T extends NodeWithAnnotations<?> & NodeWithRange<?>> Modification visit(T node);

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Change)) {
      return false;
    }
    Change other = (Change) o;
    return Objects.equals(location, other.location) && Objects.equals(annotation, other.annotation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(location, annotation);
  }

  @SuppressWarnings("unchecked")
  public JSONObject getJson() {
    JSONObject res = new JSONObject();
    res.put("LOCATION", location.accept(new LocationToJsonVisitor(), null));
    res.put("ANNOTATION", annotation);
    return res;
  }

  @Override
  public String toString() {
    return location.toString();
  }
}
