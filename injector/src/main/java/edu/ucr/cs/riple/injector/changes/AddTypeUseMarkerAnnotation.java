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
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.NodeWithRange;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import edu.ucr.cs.riple.injector.Helper;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.modifications.Insertion;
import edu.ucr.cs.riple.injector.modifications.Modification;
import edu.ucr.cs.riple.injector.modifications.MultiPositionModification;
import java.util.HashSet;
import java.util.Set;
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
public class AddTypeUseMarkerAnnotation extends AddMarkerAnnotation {

  public AddTypeUseMarkerAnnotation(Location location, String annotation) {
    super(location, annotation);
  }

  @Override
  @Nullable
  public <T extends NodeWithAnnotations<?> & NodeWithRange<?>>
      Modification computeTextModificationOn(T node) {
    if (node.getRange().isEmpty()) {
      return null;
    }

    AnnotationExpr annotationExpr = new MarkerAnnotationExpr(annotationName.simpleName);

    Set<Modification> modifications = new HashSet<>();
    Type type = Helper.getType(node);

    if (!Helper.isAnnotatedWith(type, annotationExpr)) {
      // Annotate the type.
      Range range = findTypeRange(type);
      if (range == null) {
        return null;
      }
      modifications.add(new Insertion(annotationExpr.toString(), range.begin));
    }
    // Annotate type arguments. Note: We currently do not annotate Map<K, V>[] as @Annot Map<@Annot
    // K, @Annot V>[] and will leave it as @Annot Map<K, V>[]
    if (type instanceof ClassOrInterfaceType) {
      ClassOrInterfaceType classOrInterfaceType = (ClassOrInterfaceType) type;
      if (classOrInterfaceType.getTypeArguments().isPresent()) {
        classOrInterfaceType
            .getTypeArguments()
            .get()
            .forEach(
                typeArg -> {
                  Modification onType =
                      computeTextModificationOn(
                          (NodeWithAnnotations<?> & NodeWithRange<?>) typeArg);
                  if (onType != null) {
                    modifications.add(onType);
                  }
                });
      }
    }
    return modifications.isEmpty() ? null : new MultiPositionModification(modifications);
  }

  @Override
  public RemoveAnnotation getReverse() {
    return new RemoveTypeUseMarkerAnnotation(location, annotationName.fullName);
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
   * Find the range of the given type in source code. This is used to insert the annotation before
   * the type.
   *
   * @param type the type to find its range
   * @return the range of the type or null if the type does not have a range.
   */
  private static Range findTypeRange(Type type) {
    if (type instanceof ClassOrInterfaceType) {
      ClassOrInterfaceType classOrInterfaceType = (ClassOrInterfaceType) type;
      if (classOrInterfaceType.getName().getRange().isEmpty()) {
        return null;
      }
      return ((ClassOrInterfaceType) type).getName().getRange().get();
    }
    if (type instanceof ArrayType) {
      return findTypeRange(((ArrayType) type).getComponentType());
    }
    if (type instanceof PrimitiveType) {
      if (type.getRange().isEmpty()) {
        return null;
      }
      return type.getRange().get();
    }
    throw new RuntimeException(
        "Unexpected type to get range from: " + type + " : " + type.getClass());
  }
}
