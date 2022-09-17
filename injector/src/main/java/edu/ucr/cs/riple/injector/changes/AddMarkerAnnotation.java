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

/**
 * Used to add <a
 * href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-9.html#jls-MarkerAnnotation">Marker
 * Annotation</a> on elements in source code.
 */
public class AddMarkerAnnotation extends AddAnnotation {

  public AddMarkerAnnotation(Location location, String annotation) {
    super(location, annotation);
  }

  @Override
  public void visit(NodeWithAnnotations<?> node) {
    NodeList<AnnotationExpr> annotations = node.getAnnotations();
    AnnotationExpr annotationExpr = new MarkerAnnotationExpr(annotationSimpleName);

    // Check if annot already exists.
    boolean annotAlreadyExists =
        annotations.stream().anyMatch(annot -> annot.equals(annotationExpr));
    if (annotAlreadyExists) {
      return;
    }
    node.addMarkerAnnotation(annotationSimpleName);
  }

  @Override
  public Change duplicate() {
    return new AddMarkerAnnotation(location, annotation);
  }
}