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

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import edu.ucr.cs.riple.injector.location.Location;
import java.util.Objects;
import org.json.simple.JSONObject;

public abstract class Change {
  /** Target location. */
  public final Location location;
  /** Annotation full name. */
  public final String annotation;

  public Change(Location location, String annotation) {
    this.annotation = annotation;
    this.location = location;
  }

  /**
   * Applies the change to the element in the given compilation unit tree that matches the location.
   *
   * @param tree Compilation unit tree instance.
   * @return true, if the changes was applied successfully.
   */
  public boolean apply(CompilationUnit tree) {
    return this.location.apply(tree, this);
  }

  /**
   * Visits the given node and applies the change.
   *
   * @param node Given node.
   */
  public abstract void visit(NodeWithAnnotations<?> node);

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
    res.put("LOCATION", location.getJson());
    res.put("ANNOTATION", annotation);
    return res;
  }

  public abstract Change duplicate();

  @Override
  public String toString() {
    return location.toString();
  }
}
