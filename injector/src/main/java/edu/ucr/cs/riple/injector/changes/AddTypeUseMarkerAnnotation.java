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

import static edu.ucr.cs.riple.injector.Helper.findSimpleNameRangeInTypeName;

import com.github.javaparser.Range;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.NodeWithRange;
import com.github.javaparser.ast.type.Type;
import edu.ucr.cs.riple.injector.Helper;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.modifications.Insertion;
import edu.ucr.cs.riple.injector.modifications.Modification;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Adds a type-use marker annotation to a node in the source code. It will add the annotation to
 * both the declaration and all type arguments.
 *
 * <p>For instance, for the node: {@code java.util.Map<String, java.lang.String> list;} It will add
 * the {@code @Nullable} annotation to the type {@code Map} and all the type arguments. The final
 * output will be: {@code java.util.@Nullable Map<@Nullable String, java.lang.@Nullable String>
 * list;}
 */
public class AddTypeUseMarkerAnnotation extends TypeUseAnnotationChange implements AddAnnotation {

  public AddTypeUseMarkerAnnotation(Location location, String annotation) {
    this(location, annotation, List.of(new ArrayDeque<>(List.of(0))));
  }

  public AddTypeUseMarkerAnnotation(
      Location location, String annotation, List<Deque<Integer>> typeIndex) {
    super(location, new Name(annotation), typeIndex);
  }

  @Nullable
  @Override
  public Modification computeTextModificationOnType(Type type, AnnotationExpr annotationExpr) {
    if (Helper.isAnnotatedWith(type, annotationExpr)) {
      return null;
    }
    Range range = findSimpleNameRangeInTypeName(type);
    if (range == null) {
      return null;
    }
    return new Insertion(annotationExpr.toString(), range.begin);
  }

  @Override
  public <T extends NodeWithAnnotations<?> & NodeWithRange<?>>
      Modification computeTextModificationOnNode(T node, AnnotationExpr annotationExpr) {
    boolean addOnDeclaration =
        typeIndex.stream().anyMatch(index -> index.size() == 1 && index.peek() == 0);
    Type type = Helper.getType(node);
    // For annotation on fully qualified name or inner class, the annotation is on the type. (e.g.
    // Map.@Annot Entry or java.util.@Annot Map)
    if (addOnDeclaration) {
      if (Helper.isAnnotatedWith(node, annotationExpr)) {
        return null;
      }
      // Javaparser does not know if an annotation is a type-use or declaration annotation.
      // While seeing "@Annot Object f", the type is not annotated and the annotation is on the
      // node.
      if (Helper.isAnnotatedWith(type, annotationExpr)) {
        return null;
      }
      return computeTextModificationOnType(type, annotationExpr);
    }
    return null;
  }

  @Override
  public RemoveAnnotation getReverse() {
    return new RemoveTypeUseMarkerAnnotation(location, annotationName.fullName, typeIndex);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof AddTypeUseMarkerAnnotation)) {
      return false;
    }
    AddTypeUseMarkerAnnotation otherAdd = (AddTypeUseMarkerAnnotation) other;
    return this.location.equals(otherAdd.location)
        && this.annotationName.equals(otherAdd.annotationName);
  }

  /**
   * Converts this change to a declaration change. This is used to apply the change to the
   * declaration only.
   *
   * @return a declaration change that adds the annotation to the declaration.
   */
  public AddMarkerAnnotation toDeclaration() {
    return new AddMarkerAnnotation(location, annotationName.simpleName);
  }
}
