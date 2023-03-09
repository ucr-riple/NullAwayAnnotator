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
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import edu.ucr.cs.riple.scanner.Config;
import edu.ucr.cs.riple.scanner.Serializer;
import edu.ucr.cs.riple.scanner.SymbolUtil;
import edu.ucr.cs.riple.scanner.generatedcode.SourceType;
import javax.annotation.Nullable;

/** Represents an impacted region for some class member (a field or a method). */
public class ImpactedRegion {

  /** Symbol of the member. */
  private final Symbol memberSymbol;
  /** Symbol of the enclosing class of the impacted region. */
  @Nullable private final Symbol.ClassSymbol regionClass;
  /** Symbol of the impacted region. */
  @Nullable private final Symbol regionMember;
  /** Source type of the impacted region. */
  private final SourceType source;

  /**
   * Construct an ImpactedRegion
   *
   * @param config scanner configuration
   * @param memberSymbol symbol for the class member
   * @param path path to the AST node that uses or overrides the member; the impacted region
   *     information is computed from the leaf of this path
   */
  public ImpactedRegion(Config config, Symbol memberSymbol, TreePath path) {
    this.memberSymbol = memberSymbol;
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
    Symbol enclosingClass = memberSymbol.enclClass();
    return String.join(
        "\t",
        Serializer.serializeSymbol(regionClass),
        Serializer.serializeSymbol(regionMember),
        Serializer.serializeSymbol(memberSymbol),
        Serializer.serializeSymbol(enclosingClass),
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
