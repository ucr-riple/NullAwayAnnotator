/*
 * MIT License
 *
 * Copyright (c) 2020 anonymous
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

package com.example.too.scanner.out;

import com.example.too.scanner.generatedcode.SourceType;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.example.too.scanner.Config;
import com.example.too.scanner.SymbolUtil;

import javax.annotation.Nullable;

/** Container class for storing regions where a node has been used. */
public class TrackerNode {

  /** Symbol of the used node. */
  private final Symbol usedNode;
  /** Symbol of the enclosing class of the region. */
  @Nullable private final Symbol.ClassSymbol regionClass;
  /** Symbol of the region. */
  @Nullable private final Symbol regionMember;
  /** Source type of the region. */
  private final SourceType source;

  public TrackerNode(Config config, Symbol usedNode, TreePath path) {
    this.usedNode = usedNode;
    ClassTree enclosingClass =
        path.getLeaf() instanceof ClassTree
            ? (ClassTree) path.getLeaf()
            : ASTHelpers.findEnclosingNode(path, ClassTree.class);
    if (enclosingClass != null) {
      this.regionClass = ASTHelpers.getSymbol(enclosingClass);
      this.regionMember = SymbolUtil.locateRegionMemberForSymbolAtPath(path, this.regionClass);
    } else {
      this.regionClass = null;
      this.regionMember = null;
    }
    this.source = config.getSymbolSourceResolver().getSourceForSymbolAtPath(path);
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
