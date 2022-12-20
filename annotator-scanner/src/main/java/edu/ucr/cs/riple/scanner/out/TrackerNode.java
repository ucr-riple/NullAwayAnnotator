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

package edu.ucr.cs.riple.scanner.out;

import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import edu.ucr.cs.riple.scanner.Config;
import edu.ucr.cs.riple.scanner.generatedcode.SourceType;

/** Container class for storing regions where a node has been used. */
public class TrackerNode {

  /** Symbol of the used node. */
  private final Symbol usedNode;
  /** Symbol of the enclosing class of the region. */
  private final Symbol.ClassSymbol regionClass;
  /** Symbol of the region. */
  private final Symbol regionMember;
  /**
   * Denotes if the code exists in source code or is generated. If code exists in source code it
   * will have the value {@code "SOURCE"} and if generated, it will have the name of the generator.
   */
  private final SourceType source;

  public TrackerNode(Config config, Symbol usedNode, TreePath path) {
    this.usedNode = usedNode;
    ClassTree enclosingClass;
    MethodTree enclosingMethod;
    enclosingMethod =
        path.getLeaf() instanceof MethodTree
            ? (MethodTree) path.getLeaf()
            : ASTHelpers.findEnclosingNode(path, MethodTree.class);
    enclosingClass =
        path.getLeaf() instanceof ClassTree
            ? (ClassTree) path.getLeaf()
            : ASTHelpers.findEnclosingNode(path, ClassTree.class);
    if (enclosingClass != null) {
      Symbol.ClassSymbol classSymbol = ASTHelpers.getSymbol(enclosingClass);
      regionClass = classSymbol;
      if (enclosingMethod != null) {
        Symbol.MethodSymbol methodSymbol = ASTHelpers.getSymbol(enclosingMethod);
        if (!methodSymbol.isEnclosedBy(classSymbol)) {
          enclosingMethod = null;
        }
      }
      if (enclosingMethod != null) {
        regionMember = ASTHelpers.getSymbol(enclosingMethod);
      } else {
        // Node is not enclosed by any method, can be a field declaration or enclosed by it.
        Symbol sym = ASTHelpers.getSymbol(path.getLeaf());
        Symbol.VarSymbol fieldSymbol = null;
        if (sym != null && sym.getKind().isField() && sym.isEnclosedBy(classSymbol)) {
          // Directly on a field declaration.
          fieldSymbol = (Symbol.VarSymbol) sym;
        } else {
          // Can be enclosed by a field declaration tree.
          VariableTree fieldDeclTree = ASTHelpers.findEnclosingNode(path, VariableTree.class);
          if (fieldDeclTree != null) {
            fieldSymbol = ASTHelpers.getSymbol(fieldDeclTree);
          }
        }
        if (fieldSymbol != null && fieldSymbol.isEnclosedBy(classSymbol)) {
          regionMember = fieldSymbol;
        } else {
          regionMember = null;
        }
      }
    } else {
      // just to make them final in declaration.
      regionMember = null;
      regionClass = null;
    }
    this.source = config.getSourceForSymbolAtPath(path);
  }

  @Override
  public String toString() {
    if (regionClass == null) {
      return "";
    }
    Symbol enclosingClass = usedNode.enclClass();
    return String.join(
        "\t",
        regionClass.flatName(),
        ((regionMember == null) ? "null" : regionMember.toString()),
        usedNode.toString(),
        ((enclosingClass == null) ? "null" : enclosingClass.flatName()),
        source.name());
  }

  /**
   * Returns header of the file where all these instances will be serialized.
   *
   * @return Header of target file.
   */
  public static String header() {
    return "REGION_CLASS"
        + '\t'
        + "REGION_MEMBER"
        + '\t'
        + "USED_MEMBER"
        + '\t'
        + "USED_CLASS"
        + '\t'
        + "SOURCE_TYPE";
  }
}
