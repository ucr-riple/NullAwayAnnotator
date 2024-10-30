/*
 * MIT License
 *
 * Copyright (c) 2024 Nima Karimipour
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

package edu.ucr.cs.riple.injector.util;

import com.github.javaparser.JavaToken;
import com.github.javaparser.Range;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.WildcardType;
import com.google.common.base.Preconditions;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Utility class for working with types in Java and Javaparser. */
public class TypeUtils {

  /**
   * Extracts the type of the given node implementing {@link NodeWithAnnotations}.
   *
   * @param node the node.
   * @return the type of the node.
   */
  public static Type getTypeFromNode(NodeWithAnnotations<?> node) {
    if (node instanceof MethodDeclaration) {
      return ((MethodDeclaration) node).getType();
    }
    if (node instanceof FieldDeclaration) {
      FieldDeclaration fd = (FieldDeclaration) node;
      Preconditions.checkArgument(!fd.getVariables().isEmpty());
      return fd.getVariables().get(0).getType();
    }
    if (node instanceof VariableDeclarationExpr) {
      NodeList<VariableDeclarator> decls = ((VariableDeclarationExpr) node).getVariables();
      for (VariableDeclarator v : decls) {
        // All declared variables in a VariableDeclarationExpr have the same type.
        // (e.g. Foo a, b, c;)
        return v.getType();
      }
    }
    if (node instanceof Parameter) {
      return ((Parameter) node).getType();
    }
    if (node instanceof VariableDeclarator) {
      return ((VariableDeclarator) node).getType();
    }
    if (node instanceof Type) {
      return ((Type) node);
    }
    return null;
  }

  /**
   * Helper method to check if a type is annotated with a specific annotation.
   *
   * @param type the type to check its annotations.
   * @param expr the annotation to check.
   * @return true if the node is annotated with the annotation.
   */
  public static boolean isAnnotatedWith(Type type, AnnotationExpr expr) {
    if (type instanceof WildcardType) {
      Optional<ReferenceType> extendedType = ((WildcardType) type).getExtendedType();
      return extendedType.isPresent() && isAnnotatedWith(extendedType.get(), expr);
    }
    return type.getAnnotations().stream().anyMatch(annot -> annot.getName().equals(expr.getName()));
  }

  /**
   * Helper method to check if a node is annotated with a specific annotation.
   *
   * @param node the node to check its annotations.
   * @param expr the annotation to check.
   * @return true if the node is annotated with the annotation.
   */
  public static boolean isAnnotatedWith(NodeWithAnnotations<?> node, AnnotationExpr expr) {
    return node.getAnnotations().stream().anyMatch(annot -> annot.getName().equals(expr.getName()));
  }

  /**
   * Finds the range of the simple name in the fully qualified name of the given type in the source
   * code. This is used to insert the type use annotations before the type simple name.
   *
   * @param type the type to find its range
   * @return the range of the type or null if the type does not have a range.
   */
  public static Range findSimpleNameRangeInTypeName(Type type) {
    if (type instanceof ClassOrInterfaceType) {
      ClassOrInterfaceType classOrInterfaceType = (ClassOrInterfaceType) type;
      if (classOrInterfaceType.getName().getRange().isEmpty()) {
        return null;
      }
      return ((ClassOrInterfaceType) type).getName().getRange().get();
    }
    if (type instanceof ArrayType) {
      Optional<TokenRange> tokenRange = type.getTokenRange();
      if (tokenRange.isEmpty()) {
        return null;
      }
      for (JavaToken javaToken : tokenRange.get()) {
        if (javaToken.asString().equals("[")) {
          return javaToken.getRange().orElse(null);
        }
      }
      return null;
    }
    if (type instanceof PrimitiveType) {
      if (type.getRange().isEmpty()) {
        return null;
      }
      return type.getRange().get();
    }
    if (type instanceof WildcardType) {
      if (((WildcardType) type).getExtendedType().isEmpty()) {
        // type is simply: "?"
        return type.getRange().get();
      }
      return findSimpleNameRangeInTypeName(((WildcardType) type).getExtendedType().orElse(null));
    }
    throw new RuntimeException(
        "Unexpected type to get range from: " + type + " : " + type.getClass());
  }

  /**
   * Retrieves the types associated with the given node. If the node is a class or interface
   * declaration, it returns the implemented and extended types. If the node is an object creation
   * expression, it returns the type of the object being created. In all other cases, an empty set
   * is returned.
   *
   * @param node the node from which to extract types, typically a {@code BodyDeclaration<?>} (like
   *     a class or interface declaration) or an {@code ObjectCreationExpr}.
   * @return a set of {@code ClassOrInterfaceType} representing the implemented, extended, or
   *     instantiated types, or an empty set if the node does not contain relevant type information.
   */
  public static Set<ClassOrInterfaceType> getEnclosingOrInstantiatedTypes(Node node) {
    Stream<ClassOrInterfaceType> typeStream = null;
    if (node instanceof BodyDeclaration<?>) {
      BodyDeclaration<?> enclosingClass = (BodyDeclaration<?>) node;
      if (enclosingClass instanceof ClassOrInterfaceDeclaration) {
        ClassOrInterfaceDeclaration declaration = (ClassOrInterfaceDeclaration) enclosingClass;
        typeStream =
            Stream.concat(
                declaration.getImplementedTypes().stream(),
                declaration.getExtendedTypes().stream());
      }
    }
    if (node instanceof ObjectCreationExpr) {
      typeStream = Stream.of(((ObjectCreationExpr) node).getType());
    }
    return typeStream == null ? Set.of() : typeStream.collect(Collectors.toSet());
  }
}
