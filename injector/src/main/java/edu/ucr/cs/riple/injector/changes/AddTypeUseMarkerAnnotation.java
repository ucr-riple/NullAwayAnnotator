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
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import edu.ucr.cs.riple.injector.Helper;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.modifications.Insertion;
import edu.ucr.cs.riple.injector.modifications.Modification;
import edu.ucr.cs.riple.injector.modifications.MultiPositionModification;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;

/** Used to add type-use marker annotations on elements in source code. */
public class AddTypeUseMarkerAnnotation extends AddMarkerAnnotation {

  public AddTypeUseMarkerAnnotation(Location location, String annotation) {
    super(location, annotation);
  }

  @Override
  @Nullable
  public <T extends NodeWithAnnotations<?> & NodeWithRange<?>> Modification computeModificationOn(
      T node) {
    if (node.getRange().isEmpty()) {
      return null;
    }
    Range range = node.getRange().get();
    NodeList<AnnotationExpr> annotations = node.getAnnotations();
    AnnotationExpr annotationExpr = new MarkerAnnotationExpr(annotationSimpleName);

    Set<Modification> modifications = new HashSet<>();

    // Check if annot already exists.
    boolean annotAlreadyExists =
        annotations.stream().anyMatch(annot -> annot.equals(annotationExpr));
    if (!annotAlreadyExists) {
      // Declaration might have an annotation initially but still need to annotate type-use
      // parameters if exists. Hence, we don't return here.
      modifications.add(new Insertion(annotationExpr.toString(), range.begin));
    }
    Type type = Helper.getType(node);
    // Note: We currently do not annotate Map<K, V>[] as @Annot Map<@Annot K, @Annot V>[] and will
    // leave it as @Annot Map<K, V>[]
    if (type instanceof ClassOrInterfaceType) {
      ClassOrInterfaceType classOrInterfaceType = (ClassOrInterfaceType) type;
      if (classOrInterfaceType.getTypeArguments().isPresent()) {
        classOrInterfaceType
            .getTypeArguments()
            .get()
            .forEach(
                typeArg -> {
                  Modification onType =
                      computeModificationOn((NodeWithAnnotations<?> & NodeWithRange<?>) typeArg);
                  if (onType != null) {
                    modifications.add(onType);
                  }
                });
      }
    }
    return modifications.isEmpty() ? null : new MultiPositionModification(modifications);
  }

  @Override
  public RemoveAnnotation getRemoveAnnotation() {
    return new RemoveTypeUseAnnotation(location, annotation);
  }
}
