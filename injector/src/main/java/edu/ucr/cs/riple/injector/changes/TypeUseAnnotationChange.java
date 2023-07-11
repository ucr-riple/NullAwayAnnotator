/*
 * Copyright (c) 2023 University of California, Riverside.
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

import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.NodeWithRange;
import com.github.javaparser.ast.type.Type;
import com.google.common.collect.ImmutableList;
import edu.ucr.cs.riple.injector.Helper;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.modifications.Modification;
import edu.ucr.cs.riple.injector.modifications.MultiPositionModification;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;

/** Represents a type-use annotation change on types in the AST. */
public abstract class TypeUseAnnotationChange extends AnnotationChange {

  /** List of indices that represent the position of the type argument in the node's type. */
  protected final ImmutableList<ImmutableList<Integer>> typeIndex;

  public TypeUseAnnotationChange(
      Location location, Name annotation, ImmutableList<ImmutableList<Integer>> typeIndex) {
    super(location, annotation);
    this.typeIndex = typeIndex;
  }

  /**
   * Computes the text modification on the given type argument. It does not modify the containing
   * type arguments of the given type.
   */
  @Nullable
  public abstract Modification computeTextModificationOnType(
      Type type, AnnotationExpr annotationExpr);

  /**
   * Computes the text modification on the given node. It does not modify the containing type
   * arguments.
   */
  public abstract <T extends NodeWithAnnotations<?> & NodeWithRange<?>>
      Modification computeTextModificationOnNode(T node, AnnotationExpr annotationExpr);

  @Nullable
  @Override
  public <T extends NodeWithAnnotations<?> & NodeWithRange<?>>
      Modification computeTextModificationOn(T node) {
    Set<Modification> modifications = new HashSet<>();
    AnnotationExpr annotationExpr = new MarkerAnnotationExpr(annotationName.simpleName);
    Type type = Helper.getType(node);
    Modification onNode = computeTextModificationOnNode(node, annotationExpr);
    if (onNode != null) {
      modifications.add(onNode);
    }
    // Check if the expression is a variable declaration with an initializer.
    Type initializedType = null;
    if (node instanceof VariableDeclarationExpr) {
      VariableDeclarationExpr vde = (VariableDeclarationExpr) node;
      if (vde.getVariables().size() > 0) {
        if (vde.getVariables().get(0).getInitializer().isPresent()) {
          Expression initializedValue = vde.getVariables().get(0).getInitializer().get();
          if (initializedValue instanceof ObjectCreationExpr) {
            initializedType = ((ObjectCreationExpr) initializedValue).getType();
          }
        }
      }
    }

    for (ImmutableList<Integer> index : typeIndex) {
      Deque<Integer> cc = new ArrayDeque<>(index);
      if (cc.size() == 1 && cc.peek() == 0) {
        // Already added on declaration.
        continue;
      }
      // copy index to a new deque
      Deque<Integer> copy = new ArrayDeque<>(cc);
      // Apply the change on type arguments.
      modifications.addAll(type.accept(new TypeArgumentChangeVisitor(cc, annotationExpr), this));
      if (initializedType != null) {
        modifications.addAll(
            initializedType.accept(new TypeArgumentChangeVisitor(copy, annotationExpr), this));
      }
    }

    return modifications.isEmpty() ? null : new MultiPositionModification(modifications);
  }

  @Override
  public boolean equals(Object o) {
    boolean ans = super.equals(o);
    if (!(o instanceof TypeUseAnnotationChange)) {
      return false;
    }
    if (!ans) {
      return false;
    }
    return typeIndex.equals(((TypeUseAnnotationChange) o).typeIndex);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), typeIndex);
  }
}
