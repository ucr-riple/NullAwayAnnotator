/*
 * MIT License
 *
 * Copyright (c) 2023 Nima Karimipour
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

package edu.ucr.cs.riple.injector.visitors;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.WildcardType;
import com.github.javaparser.ast.visitor.GenericVisitorWithDefaults;
import edu.ucr.cs.riple.injector.changes.Change;
import edu.ucr.cs.riple.injector.modifications.Modification;
import java.util.HashSet;
import java.util.Set;

/** Visitor for making changes on the type and all of its type arguments. */
public class TypeChangeVisitor extends GenericVisitorWithDefaults<Set<Modification>, Change> {

  @Override
  public Set<Modification> visit(PrimitiveType n, Change change) {
    if (n.getRange().isEmpty()) {
      return Set.of();
    }
    Modification modification = change.computeModificationOn(n);
    return modification == null ? Set.of() : Set.of(modification);
  }

  @Override
  public Set<Modification> visit(ClassOrInterfaceType classOrInterfaceType, Change change) {
    Set<Modification> result = new HashSet<>();
    Modification modification = change.computeModificationOn(classOrInterfaceType);
    if (modification != null) {
      result.add(modification);
    }
    if (classOrInterfaceType.getTypeArguments().isPresent()) {
      classOrInterfaceType
          .getTypeArguments()
          .get()
          .forEach(e -> result.addAll(e.accept(this, change)));
    }
    return result;
  }

  @Override
  public Set<Modification> visit(WildcardType type, Change change) {
    Set<Modification> result = new HashSet<>();
    Modification modification = change.computeModificationOn(type);
    if (modification != null) {
      result.add(modification);
    }
    if (type.getExtendedType().isPresent()) {
      result.addAll(type.getExtendedType().get().accept(this, change));
    }
    if (type.getSuperType().isPresent()) {
      result.addAll(type.getSuperType().get().accept(this, change));
    }
    return result;
  }

  @Override
  public Set<Modification> visit(ArrayType type, Change change) {
    Set<Modification> result = new HashSet<>(type.getComponentType().accept(this, change));
    Modification modification = change.computeModificationOn(type);
    if (modification != null) {
      result.add(modification);
    }
    return result;
  }

  @Override
  public Set<Modification> defaultAction(NodeList n, Change arg) {
    throw new RuntimeException("Unexpected type in TypeChangeVisitor: " + n.getClass().getName());
  }

  @Override
  public Set<Modification> defaultAction(Node n, Change arg) {
    throw new RuntimeException("Unexpected type in TypeChangeVisitor: " + n.getClass().getName());
  }
}
