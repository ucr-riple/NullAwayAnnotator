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

package edu.ucr.cs.riple.scanner.scanners;

import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import javax.lang.model.element.ElementKind;

public class OriginScanner extends AccumulatorScanner<Symbol> {

  @Override
  public Set<Symbol> visitAssignment(AssignmentTree node, Symbol unused) {
    Tree variable = node.getVariable();
    Symbol symbol = ASTHelpers.getSymbol(variable);
    if (symbol != null && symbol.equals(unused)) {
      return node.getExpression().accept(new ExpressionToSymbolScanner(), null);
    }
    return Set.of();
  }

  @Override
  public Set<Symbol> visitVariable(VariableTree node, Symbol target) {
    Symbol symbol = ASTHelpers.getSymbol(node);
    if (symbol != null && symbol.equals(target)) {
      if (node.getInitializer() == null) {
        return Set.of();
      }
      return node.getInitializer().accept(new ExpressionToSymbolScanner(), null);
    }
    return Set.of();
  }

  @Override
  public Set<Symbol> visitEnhancedForLoop(EnhancedForLoopTree node, Symbol unused) {
    return node.getExpression().accept(this, unused);
  }

  public Set<Symbol> scanVariable(MethodTree tree, Symbol target) {
    if (target.getKind() != ElementKind.LOCAL_VARIABLE) {
      return Set.of(target);
    }
    Set<Symbol> result = new HashSet<>();
    Queue<Symbol> queue = new ArrayDeque<>();
    queue.add(target);
    Set<Symbol> visited = new HashSet<>();
    while (!queue.isEmpty()) {
      Symbol current = queue.poll();
      if (visited.contains(current)) {
        continue;
      }
      visited.add(current);
      Set<Symbol> involvedSymbols = tree.accept(this, current);
      involvedSymbols.forEach(
          symbol -> {
            if (symbol.getKind() == ElementKind.LOCAL_VARIABLE) {
              if (!visited.contains(symbol)) {
                queue.add(symbol);
              }
            } else {
              result.add(symbol);
            }
          });
    }
    return result;
  }
}
