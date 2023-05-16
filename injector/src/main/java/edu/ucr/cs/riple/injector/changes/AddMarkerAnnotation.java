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

import com.github.javaparser.Range;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.NodeWithRange;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.modifications.Insertion;
import edu.ucr.cs.riple.injector.modifications.Modification;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Used to add <a
 * href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-9.html#jls-MarkerAnnotation">Marker
 * Annotation</a> on elements in source code.
 */
public class AddMarkerAnnotation extends AnnotationChange implements AddAnnotation {

  public AddMarkerAnnotation(Location location, String annotation) {
    super(location, new Name(annotation));
  }

  @Override
  @Nullable
  public <T extends NodeWithAnnotations<?> & NodeWithRange<?>>
      Modification computeTextModificationOn(T node) {
    if (node.getRange().isEmpty()) {
      return null;
    }
    Range range = node.getRange().get();
    NodeList<AnnotationExpr> annotations = node.getAnnotations();
    AnnotationExpr annotationExpr = new MarkerAnnotationExpr(annotationName.simpleName);

    // Check if annot already exists.
    boolean annotAlreadyExists =
        annotations.stream().anyMatch(annot -> annot.equals(annotationExpr));
    if (annotAlreadyExists) {
      return null;
    }
    return new Insertion(annotationExpr.toString(), range.begin);
  }

  @Override
  public RemoveAnnotation getReverse() {
    return new RemoveMarkerAnnotation(location, annotationName.fullName);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AddMarkerAnnotation)) {
      return false;
    }
    AddMarkerAnnotation that = (AddMarkerAnnotation) o;
    return location.equals(that.location) && annotationName.equals(that.annotationName);
  }

  @Override
  public int hashCode() {
    return Objects.hash("ADD", location, annotationName);
  }
}
