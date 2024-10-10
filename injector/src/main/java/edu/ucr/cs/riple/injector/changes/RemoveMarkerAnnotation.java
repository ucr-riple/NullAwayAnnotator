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

import com.github.javaparser.Range;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.NodeWithRange;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.modifications.Deletion;
import edu.ucr.cs.riple.injector.modifications.Modification;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Used to remove <a
 * href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-9.html#jls-MarkerAnnotation">Marker
 * Annotation</a> on elements in source code.
 */
public class RemoveMarkerAnnotation extends AnnotationChange implements RemoveAnnotation {

  public RemoveMarkerAnnotation(Location location, String annotation) {
    super(location, new Name(annotation));
  }

  @Override
  @Nullable
  public <T extends NodeWithAnnotations<?> & NodeWithRange<?>>
      Modification computeTextModificationOn(T node) {
    // We only insert annotations with their simple name, therefore, we should only remove
    // the annotation if it matches with the simple name (otherwise, the annotation was not injected
    // by the core module request and should not be touched).
    AnnotationExpr annotationExpr = new MarkerAnnotationExpr(annotationName.simpleName);
    for (AnnotationExpr expr : node.getAnnotations()) {
      if (expr.equals(annotationExpr)) {
        Optional<Range> annotRange = expr.getRange();
        return annotRange
            .map(value -> new Deletion(expr.toString(), value.begin, value.end))
            .orElse(null);
      }
    }
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RemoveMarkerAnnotation)) {
      return false;
    }
    RemoveMarkerAnnotation other = (RemoveMarkerAnnotation) o;
    return Objects.equals(location, other.location)
        && Objects.equals(annotationName, other.annotationName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(RemoveMarkerAnnotation.class, location, annotationName);
  }

  @Override
  public ASTChange copy() {
    return new RemoveMarkerAnnotation(location, annotationName.fullName);
  }
}
