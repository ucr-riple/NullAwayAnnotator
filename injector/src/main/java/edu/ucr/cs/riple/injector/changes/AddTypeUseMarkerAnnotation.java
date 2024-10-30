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
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.OnLocalVariable;
import edu.ucr.cs.riple.injector.modifications.Insertion;
import edu.ucr.cs.riple.injector.modifications.Modification;
import edu.ucr.cs.riple.injector.util.TypeUtils;
import java.util.Objects;
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
    this(location, annotation, ImmutableList.of(ImmutableList.of(0)));
  }

  public AddTypeUseMarkerAnnotation(
      Location location, String annotation, ImmutableList<ImmutableList<Integer>> typeIndex) {
    super(location, new Name(annotation), typeIndex);
  }

  @Nullable
  @Override
  public Modification computeTextModificationOnType(Type type, AnnotationExpr annotationExpr) {
    if (TypeUtils.isAnnotatedWith(type, annotationExpr)) {
      return null;
    }
    Range range = TypeUtils.findSimpleNameRangeInTypeName(type);
    if (range == null) {
      return null;
    }
    return new Insertion(annotationExpr.toString(), range.begin);
  }

  @Override
  public <T extends NodeWithAnnotations<?> & NodeWithRange<?>>
      Modification computeTextModificationOnNode(T node, AnnotationExpr annotationExpr) {
    boolean addOnDeclaration =
        typeIndex.stream().anyMatch(index -> index.size() == 1 && index.get(0) == 0);
    Type type = TypeUtils.getTypeFromNode(node);
    // For annotation on fully qualified name or inner class, the annotation is on the type. (e.g.
    // Map.@Annot Entry or java.util.@Annot Map)
    if (addOnDeclaration) {
      if (TypeUtils.isAnnotatedWith(node, annotationExpr)) {
        return null;
      }
      // Javaparser does not know if an annotation is a type-use or declaration annotation.
      // While seeing "@Annot Object f", the type is not annotated and the annotation is on the
      // node.
      if (TypeUtils.isAnnotatedWith(type, annotationExpr)) {
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

  /**
   * Converts this change to a declaration change. This is used to apply the change to the
   * declaration only.
   *
   * @return a declaration change that adds the annotation to the declaration.
   */
  @Nullable
  public AddTypeUseMarkerAnnotation toDeclaration() {
    // check if the annotation is on array declaration
    // TODO check this for array.
    if (isOnLocalVariableArray() && typeIndex.contains(ImmutableList.of(0))) {
      return null;
    }
    if (typeIndex.contains(ImmutableList.of(0))) {
      return new AddTypeUseMarkerAnnotation(location, annotationName.simpleName);
    } else {
      return null;
    }
  }

  @Override
  public boolean equals(Object other) {
    boolean ans = super.equals(other);
    if (!ans) {
      return false;
    }
    return other instanceof AddTypeUseMarkerAnnotation;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), AddTypeUseMarkerAnnotation.class);
  }

  @Override
  public String toString() {
    return super.toString() + ", index: " + typeIndex;
  }

  public boolean isOnLocalVariableArray() {
    if (!location.isOnLocalVariable()) {
      return false;
    }
    OnLocalVariable onLocalVariable = (OnLocalVariable) location;
    return onLocalVariable.isOnArray;
  }

  @Override
  public ASTChange copy() {
    return new AddTypeUseMarkerAnnotation(location, annotationName.fullName, typeIndex);
  }
}
