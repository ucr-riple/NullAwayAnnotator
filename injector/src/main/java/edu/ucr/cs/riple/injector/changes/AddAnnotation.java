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
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import edu.ucr.cs.riple.injector.Helper;
import edu.ucr.cs.riple.injector.location.Location;
import javax.annotation.Nullable;
import org.json.simple.JSONObject;

public class AddAnnotation extends Change {

  /** Argument of the annotation. If null, the added annotation will be a marker annotation. */
  @Nullable private final String argument;

  public AddAnnotation(Location location, String annotation, @Nullable String argument) {
    super(location, annotation);
    this.argument = argument;
  }

  public AddAnnotation(Location location, String annotation) {
    this(location, annotation, null);
  }

  @Override
  public void visit(NodeWithAnnotations<?> node) {
    final String annotSimpleName = Helper.simpleName(annotation);
    NodeList<AnnotationExpr> annotations = node.getAnnotations();
    AnnotationExpr annotationExpr =
        this.argument == null
            ? new MarkerAnnotationExpr(annotSimpleName)
            : new SingleMemberAnnotationExpr(
                new Name(annotSimpleName), new StringLiteralExpr(argument));
    boolean exists = annotations.stream().anyMatch(annot -> annot.equals(annotationExpr));
    if (!exists) {
      if (argument == null || argument.equals("")) {
        node.addMarkerAnnotation(annotSimpleName);
      } else {
        node.addAnnotation(annotationExpr);
      }
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public JSONObject getJson() {
    JSONObject res = super.getJson();
    res.put("INJECT", true);
    return res;
  }

  @Override
  public Change duplicate() {
    return new AddAnnotation(location.duplicate(), annotation, argument);
  }
}
