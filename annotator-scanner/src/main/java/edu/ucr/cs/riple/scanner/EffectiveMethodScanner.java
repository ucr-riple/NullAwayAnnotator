/*
 * MIT License
 *
 * Copyright (c) 2025 Nima Karimipour
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

package edu.ucr.cs.riple.scanner;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.ElementKind;

public class EffectiveMethodScanner {

  /** The parameters of the method being scanned. */
  private final ImmutableList<Symbol.VarSymbol> parameters;

  /** The scanner used to scan the method for identifiers that are methods arguments */
  private final IdentifierScanner scanner;

  /** Scans the method for identifiers and counts the number of times each parameter is used. */
  final class IdentifierScanner extends TreeScanner<Void, Set<Symbol>> {
    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Set<Symbol> unused) {
      // to skip visiting method arguments, we only visit the method select
      scan(node.getMethodSelect(), unused);
      return null;
    }

    @Override
    public Void visitIdentifier(IdentifierTree node, Set<Symbol> effectiveParameters) {
      Symbol symbol = ASTHelpers.getSymbol(node);
      if (!(symbol instanceof Symbol.VarSymbol)) {
        return null;
      }
      if (symbol.getKind() == ElementKind.PARAMETER && parameters.contains(symbol)) {
        effectiveParameters.add(symbol);
      }
      return null;
    }
  }

  public EffectiveMethodScanner(Symbol.MethodSymbol methodSymbol) {
    this.parameters = ImmutableList.copyOf(methodSymbol.getParameters());
    this.scanner = new IdentifierScanner();
  }

  /**
   * Scans the method for identifiers that are method parameters and returns the set of effective
   * parameters.
   *
   * @param tree the method to scan
   * @return the set of effective parameters
   */
  public Set<Symbol.VarSymbol> scanMethod(MethodTree tree) {
    if (tree.getBody() == null || tree.getParameters().isEmpty()) {
      // Method is abstract or has no parameter, it cannot be effective to any parameter.
      return Set.of();
    }
    Set<Symbol> effectiveParameters = new HashSet<>();
    scanner.scan(tree, effectiveParameters);
    return effectiveParameters.stream()
        .map(symbol -> (Symbol.VarSymbol) symbol)
        .collect(Collectors.toSet());
  }
}
