/*
 * MIT License
 *
 * Copyright (c) 2022 Nima Karimipour
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

package edu.ucr.cs.riple.scanner.generatedcode;

import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import edu.ucr.cs.riple.scanner.SymbolUtil;

/**
 * Detector for generated code by <a href="https://projectlombok.org">Lombok</a>. The basic
 * assumption used here is that all generated code by Lombok is inside a method annotated with <a
 * href="https://projectlombok.org/api/lombok/Generated">@lombok.Generated</a> annotation. Please
 * note that {@code "lombok.addLombokGeneratedAnnotation = true"} should be set in lombok
 * configurations while building target module to add the required annotations.
 */
public class LombokGeneratedCodeDetector implements GeneratedCodeDetector {

  /** Associated name for Lombok in region serializations in <i>SOURCE_TYPE</i> column. */
  private static final String NAME = "LOMBOK";

  @Override
  public boolean isGeneratedCode(TreePath path) {
    MethodTree enclosingMethod =
        path.getLeaf() instanceof MethodTree
            ? (MethodTree) path.getLeaf()
            : ASTHelpers.findEnclosingNode(path, MethodTree.class);
    if (enclosingMethod == null && path.getLeaf() instanceof MethodTree) {
      enclosingMethod = (MethodTree) path.getLeaf();
    }
    if (enclosingMethod == null) {
      return false;
    }
    Symbol.MethodSymbol methodSymbol = ASTHelpers.getSymbol(enclosingMethod);
    return SymbolUtil.getAllAnnotations(methodSymbol)
        .map(anno -> anno.getAnnotationType().toString())
        .anyMatch(s -> s.endsWith("lombok.Generated"));
  }

  @Override
  public String getCodeGeneratorName() {
    return NAME;
  }
}
