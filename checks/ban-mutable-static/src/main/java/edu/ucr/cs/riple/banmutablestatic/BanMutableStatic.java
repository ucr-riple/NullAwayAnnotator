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

package edu.ucr.cs.riple.banmutablestatic;

import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;

import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import javax.lang.model.element.Modifier;

/**
 * Checker for disallowing mutable declaration of static fields. At this moment it only covers
 * shallow mutability, deep mutability will be covered in the future.
 */
@AutoService(BugChecker.class)
@BugPattern(
    name = "BanMutableStatic",
    altNames = {"BansMutableStaticFields"},
    summary = "Bans mutable static field declaration",
    tags = BugPattern.StandardTags.STYLE,
    severity = SUGGESTION)
@SuppressWarnings("BugPatternNaming")
public class BanMutableStatic extends BugChecker implements BugChecker.VariableTreeMatcher {

  public final String ERROR_MESSAGE =
      "Variable %s is declared as a mutable static field, should be replaced with: final static %s %s";
  private final String LINK_URL = "https://github.com/nimakarimipour/NullAwayAnnotator";

  public BanMutableStatic() {}

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    boolean isStatic = tree.getModifiers().getFlags().contains(Modifier.STATIC);
    boolean isMutable = !tree.getModifiers().getFlags().contains(Modifier.FINAL);
    if (isMutable && isStatic) {
      Symbol symbol = ASTHelpers.getSymbol(tree);
      return buildDescription(tree)
          .setMessage(String.format(ERROR_MESSAGE, symbol, symbol.type, symbol))
          .setLinkUrl(LINK_URL)
          .build();
    }
    return Description.NO_MATCH;
  }
}
