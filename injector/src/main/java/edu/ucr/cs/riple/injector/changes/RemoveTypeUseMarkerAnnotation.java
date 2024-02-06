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
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.NodeWithRange;
import com.github.javaparser.ast.type.Type;
import com.google.common.collect.ImmutableList;
import edu.ucr.cs.riple.injector.Helper;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.modifications.Deletion;
import edu.ucr.cs.riple.injector.modifications.Modification;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Removes a type-use marker annotation from a node in the source code. It will remove the
 * annotation both from the declaration and all type arguments.
 *
 * <p>For instance, for the node: {@code java.util.@Nullable Map<@Nullable String,
 * java.lang.@Nullable String> list;} It will remove the {@code @Nullable} annotation from the type
 * {@code Map} and all the type arguments. The final output will be: {@code java.util.Map<String,
 * java.lang.String> list;}
 */
public class RemoveTypeUseMarkerAnnotation extends TypeUseAnnotationChange
    implements RemoveAnnotation {

  public RemoveTypeUseMarkerAnnotation(Location location, String annotation) {
    super(location, new Name(annotation), ImmutableList.of(ImmutableList.of(0)));
  }

  public RemoveTypeUseMarkerAnnotation(
      Location location, String annotation, ImmutableList<ImmutableList<Integer>> typeIndex) {
    super(location, new Name(annotation), typeIndex);
  }

  @Nullable
  @Override
  public Modification computeTextModificationOnType(Type type, AnnotationExpr annotationExpr) {
    // Remove the annotation from the type if exists e.g. java.lang.@Annot String f;
    for (AnnotationExpr expr : type.getAnnotations()) {
      if (expr.equals(annotationExpr)) {
        Optional<Range> annotRange = expr.getRange();
        if (annotRange.isPresent()) {
          return new Deletion(expr.toString(), annotRange.get().begin, annotRange.get().end);
        }
      }
    }
    return null;
  }

  @Override
  public <T extends NodeWithAnnotations<?> & NodeWithRange<?>>
      Modification computeTextModificationOnNode(T node, AnnotationExpr annotationExpr) {
    Type type = Helper.getType(node);

    boolean removeOnDeclaration =
        typeIndex.stream().anyMatch(index -> index.size() == 1 && index.get(0) == 0);
    if (removeOnDeclaration) {
      // Remove the annotation from the declaration if exists e.g. @Annot String f;
      for (AnnotationExpr expr : node.getAnnotations()) {
        if (expr.equals(annotationExpr)) {
          Optional<Range> annotRange = expr.getRange();
          if (annotRange.isPresent()) {
            return new Deletion(expr.toString(), annotRange.get().begin, annotRange.get().end);
          }
        }
      }
      Modification onType = computeTextModificationOnType(type, annotationExpr);
      if (onType != null) {
        return onType;
      } else {
        // Remove the annotation from the type if exists e.g. java.lang.@Annot String f;
        return computeTextModificationOnType(type, annotationExpr);
      }
    }
    return null;
  }

  @Override
  public boolean equals(Object o) {
    boolean ans = super.equals(o);
    if (!ans) {
      return false;
    }
    return o instanceof RemoveTypeUseMarkerAnnotation;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), RemoveTypeUseMarkerAnnotation.class);
  }

  @Override
  public ASTChange copy() {
    return new RemoveTypeUseMarkerAnnotation(location, annotationName.fullName, typeIndex);
  }
}
