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

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import edu.ucr.cs.riple.injector.location.Location;
import java.util.Objects;
import org.json.simple.JSONObject;

public class RemoveAnnotation extends Change {
  public RemoveAnnotation(Location location, String annotation) {
    super(location, annotation);
  }

  @Override
  public void visit(NodeWithAnnotations<?> node) {
    // We only insert annotations with their simple name, therefore, we should only remove
    // the annotation if it matches with the simple name (otherwise, the annotation was not injected
    // by the core module request and should not be touched). Also, we currently require removing
    // only MarkerAnnotations, removal of other types of annotations are not supported yet.
    AnnotationExpr annotationExpr = new MarkerAnnotationExpr(annotationSimpleName);
    NodeList<AnnotationExpr> annotations = node.getAnnotations();
    annotations.removeIf(annotationExpr::equals);
  }

  @Override
  public JSONObject getJson() {
    JSONObject res = super.getJson();
    res.put("INJECT", false);
    return res;
  }

  @Override
  public Change duplicate() {
    return new RemoveAnnotation(location.duplicate(), annotation);
  }

  @Override
  public boolean equals(Object other) {
    boolean superAns = super.equals(other);
    if (!superAns) {
      return false;
    }
    return other instanceof RemoveAnnotation;
  }

  @Override
  public int hashCode() {
    return Objects.hash("Remove", super.hashCode());
  }
}
