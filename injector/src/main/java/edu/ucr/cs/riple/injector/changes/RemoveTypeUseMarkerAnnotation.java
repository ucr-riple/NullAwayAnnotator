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
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import edu.ucr.cs.riple.injector.Helper;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.modifications.Deletion;
import edu.ucr.cs.riple.injector.modifications.Modification;
import edu.ucr.cs.riple.injector.modifications.MultiPositionModification;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
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
public class RemoveTypeUseMarkerAnnotation extends RemoveMarkerAnnotation {

  public RemoveTypeUseMarkerAnnotation(Location location, String annotation) {
    super(location, annotation);
  }

  @Override
  @Nullable
  public <T extends NodeWithAnnotations<?> & NodeWithRange<?>>
      Modification computeTextModificationOn(T node) {
    Type type = Helper.getType(node);
    AnnotationExpr annotationExpr = new MarkerAnnotationExpr(annotationName.simpleName);
    Set<Modification> modifications = new HashSet<>();
    // Remove the annotation from the declaration if exists e.g. @Annot String f;
    Modification onDeclaration = super.computeTextModificationOn(node);
    if (onDeclaration != null) {
      modifications.add(onDeclaration);
    } else {
      // Remove the annotation from the type if exists e.g. java.lang.@Annot String f;
      for (AnnotationExpr expr : type.getAnnotations()) {
        if (expr.equals(annotationExpr)) {
          Optional<Range> annotRange = expr.getRange();
          annotRange
              .map(value -> new Deletion(expr.toString(), value.begin, value.end))
              .ifPresent(modifications::add);
        }
      }
    }

    // Remove annotation from type arguments if exists e.g. List<@Annot String>
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
}
