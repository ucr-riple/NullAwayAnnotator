/*
 * MIT License
 *
 * Copyright (c) 2020 Nima Karimipour
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

package edu.ucr.cs.css;

import com.google.common.base.Preconditions;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.TargetType;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.lang.model.element.AnnotationMirror;

/** Helper class for working with Symbols. */
public class SymbolUtil {

  /**
   * find the closest ancestor method in a superclass or superinterface that method overrides
   *
   * @param method the subclass method
   * @param types the types data structure from javac
   * @return closest overridden ancestor method, or <code>null</code> if method does not override
   *     anything
   */
  @Nullable
  public static Symbol.MethodSymbol getClosestOverriddenMethod(
      Symbol.MethodSymbol method, Types types) {
    // taken from Error Prone MethodOverrides check
    Symbol.ClassSymbol owner = method.enclClass();
    for (Type s : types.closure(owner.type)) {
      if (types.isSameType(s, owner.type)) {
        continue;
      }
      for (Symbol m : s.tsym.members().getSymbolsByName(method.name)) {
        if (!(m instanceof Symbol.MethodSymbol)) {
          continue;
        }
        Symbol.MethodSymbol msym = (Symbol.MethodSymbol) m;
        if (msym.isStatic()) {
          continue;
        }
        if (method.overrides(msym, owner, types, /*checkReturn*/ false)) {
          return msym;
        }
      }
    }
    return null;
  }

  public static boolean hasNullableAnnotation(
      Stream<? extends AnnotationMirror> annotations, Config config) {
    return annotations
        .map(anno -> anno.getAnnotationType().toString())
        .anyMatch(anno -> isNullableAnnotation(anno, config));
  }

  /**
   * Check whether an annotation should be treated as equivalent to <code>@Nullable</code>.
   *
   * @param annotName annotation name
   * @return true if we treat annotName as a <code>@Nullable</code> annotation, false otherwise
   */
  public static boolean isNullableAnnotation(String annotName, Config config) {
    return annotName.endsWith(".Nullable")
        // endsWith and not equals and no `org.`, because gradle's shadow plug in rewrites strings
        // and will replace `org.checkerframework` with `shadow.checkerframework`. Yes, really...
        // I assume it's something to handle reflection.
        || annotName.endsWith(".checkerframework.checker.nullness.compatqual.NullableDecl")
        // matches javax.annotation.CheckForNull and edu.umd.cs.findbugs.annotations.CheckForNull
        || annotName.endsWith(".CheckForNull");
  }

  /**
   * Does the parameter of {@code symbol} at {@code paramInd} have a {@code @Nullable} declaration
   * or type-use annotation? This method works for methods defined in either source or class files.
   */
  public static boolean paramHasNullableAnnotation(
      Symbol.MethodSymbol symbol, int paramInd, Config config) {
    return hasNullableAnnotation(getAllAnnotationsForParameter(symbol, paramInd), config);
  }

  /**
   * Works for method parameters defined either in source or in class files
   *
   * @param symbol the method symbol
   * @param paramInd index of the parameter
   * @return all declaration and type-use annotations for the parameter
   */
  public static Stream<? extends AnnotationMirror> getAllAnnotationsForParameter(
      Symbol.MethodSymbol symbol, int paramInd) {
    Symbol.VarSymbol varSymbol = symbol.getParameters().get(paramInd);
    return Stream.concat(
        varSymbol.getAnnotationMirrors().stream(),
        symbol
            .getRawTypeAttributes()
            .stream()
            .filter(
                t ->
                    t.position.type.equals(TargetType.METHOD_FORMAL_PARAMETER)
                        && t.position.parameter_index == paramInd));
  }

  /**
   * find the enclosing method, lambda expression or initializer block for the leaf of some tree
   * path
   *
   * @param path the tree path
   * @return the closest enclosing method / lambda
   */
  @Nullable
  public static TreePath findEnclosingMethodOrLambdaOrInitializer(TreePath path) {
    TreePath curPath = path.getParentPath();
    while (curPath != null) {
      if (curPath.getLeaf() instanceof MethodTree
          || curPath.getLeaf() instanceof LambdaExpressionTree) {
        return curPath;
      }
      TreePath parent = curPath.getParentPath();
      if (parent != null && parent.getLeaf() instanceof ClassTree) {
        if (curPath.getLeaf() instanceof BlockTree) {
          // found initializer block
          return curPath;
        }
        if (curPath.getLeaf() instanceof VariableTree
            && ((VariableTree) curPath.getLeaf()).getInitializer() != null) {
          // found field with an inline initializer
          return curPath;
        }
      }
      curPath = parent;
    }
    return null;
  }

  /**
   * finds the corresponding functional interface method for a lambda expression or method reference
   *
   * @param tree the lambda expression or method reference
   * @return the functional interface method
   */
  public static Symbol.MethodSymbol getFunctionalInterfaceMethod(ExpressionTree tree, Types types) {
    Preconditions.checkArgument(
        (tree instanceof LambdaExpressionTree) || (tree instanceof MemberReferenceTree));
    Type funcInterfaceType = ((JCTree.JCFunctionalExpression) tree).type;
    return (Symbol.MethodSymbol) types.findDescriptorSymbol(funcInterfaceType.tsym);
  }
}
