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
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.modifications.Insertion;
import edu.ucr.cs.riple.injector.modifications.Modification;
import javax.annotation.Nullable;
import javax.lang.model.element.ElementKind;

/**
 * Used to add <a
 * href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-9.html#jls-MarkerAnnotation">Marker
 * Annotation</a> on elements in source code.
 */
public class AddMarkerAnnotation extends AddAnnotation {

  /**
   * Comment to be added on the added annotation. If {@code null} no comment will be added. Comment
   * cannot contain any consecutive characters of {@code *} and {@code /} since this comment will be
   * added as a trailing comment surrounded by "/* {@literal *}/".
   */
  @Nullable public final String comment;

  public AddMarkerAnnotation(Location location, String annotation, @Nullable String comment) {
    super(location, annotation);
    this.comment = comment;
    if (comment != null && comment.contains("*/")) {
      throw new IllegalArgumentException(
          "Comment cannot contain pair of \"*/\" characters: " + comment);
    }
  }

  public AddMarkerAnnotation(Location location, String annotation) {
    this(location, annotation, null);
  }

  @Override
  @Nullable
  public Modification visit(ElementKind kind, NodeWithAnnotations<?> node, Range range) {
    NodeList<AnnotationExpr> annotations = node.getAnnotations();
    AnnotationExpr annotationExpr = new MarkerAnnotationExpr(annotationSimpleName);

    // Check if annot already exists.
    boolean annotAlreadyExists =
        annotations.stream().anyMatch(annot -> annot.equals(annotationExpr));
    if (annotAlreadyExists) {
      return null;
    }
    String contentToAdd =
        this.comment == null
            ? annotationExpr.toString()
            : annotationExpr + " " + getCommentRepresentationInSourceCode();
    return new Insertion(contentToAdd, range.begin, kind);
  }

  @Override
  public Change duplicate() {
    return new AddMarkerAnnotation(location, annotation);
  }

  /**
   * Returns string representation of comment as trailing comment according to java comment style.
   *
   * @return Comment according to java comment style.
   */
  private String getCommentRepresentationInSourceCode() {
    if (this.comment == null) {
      // to make this method return non-null.
      throw new RuntimeException("comment is not initialized");
    }
    return "/* " + this.comment + " */";
  }
}
