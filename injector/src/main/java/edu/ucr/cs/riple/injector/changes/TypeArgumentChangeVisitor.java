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

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.NodeWithRange;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.GenericVisitorWithDefaults;
import edu.ucr.cs.riple.injector.modifications.Modification;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A visitor that computes the modifications needed to change the type arguments of a node. This
 * visitor supports the following types:
 *
 * <ul>
 *   <li>{@link ClassOrInterfaceType}
 *   <li>{@link ArrayType}
 * </ul>
 *
 * If other types are visited by this visitor, no changes will be applied. If support for further
 * types are desired, their corresponding visit methods should be overridden.
 */
public class TypeArgumentChangeVisitor
    extends GenericVisitorWithDefaults<Set<Modification>, ASTChange> {

  @Override
  public Set<Modification> visit(ClassOrInterfaceType type, ASTChange change) {
    if (type.getTypeArguments().isEmpty()) {
      return Collections.emptySet();
    }
    Set<Modification> result = new HashSet<>();
    type.getTypeArguments()
        .get()
        .forEach(
            typeArg -> {
              Modification onType =
                  change.computeTextModificationOn(
                      (NodeWithAnnotations<?> & NodeWithRange<?>) typeArg);
              if (onType != null) {
                result.add(onType);
              }
            });
    return result;
  }

  @Override
  public Set<Modification> visit(ArrayType type, ASTChange change) {
    return type.getComponentType().accept(this, change);
  }

  /** This will be called by every node visit method that is not overridden. */
  @Override
  public Set<Modification> defaultAction(Node n, ASTChange arg) {
    // For now, we do not intend to annotate any other type.
    return Collections.emptySet();
  }
}
