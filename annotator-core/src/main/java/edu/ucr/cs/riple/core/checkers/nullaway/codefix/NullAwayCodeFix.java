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

package edu.ucr.cs.riple.core.checkers.nullaway.codefix;

import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.checkers.nullaway.NullAwayError;
import edu.ucr.cs.riple.core.checkers.nullaway.codefix.agent.ChatGPT;
import edu.ucr.cs.riple.core.util.ASTUtil;
import edu.ucr.cs.riple.injector.Injector;
import edu.ucr.cs.riple.injector.changes.MethodRewriteChange;
import java.util.Set;

/** A class that provides code fixes for {@link NullAwayError}s. */
public class NullAwayCodeFix {

  /** The {@link ChatGPT} instance used to generate code fixes. */
  private final ChatGPT gpt;

  /** The {@link Injector} instance used to apply code fixes. */
  private final Injector injector;

  public NullAwayCodeFix(Config config) {
    this.gpt = new ChatGPT(config);
    this.injector = new Injector(config.languageLevel);
  }

  /**
   * Generates a code fix for the given {@link NullAwayError}. The fix is rewrites of sets of
   * methods.
   *
   * @param error the error to fix.
   * @return a {@link MethodRewriteChange} that represents the code fix, or {@code null} if the
   *     error cannot be fixed.
   */
  public Set<MethodRewriteChange> fix(NullAwayError error) {
    switch (error.messageType) {
      case "DEREFERENCE_NULLABLE":
        return resolveDereferenceError(error);
      default:
        return Set.of();
    }
  }

  /**
   * Resolves a dereference error by generating a code fix.
   *
   * @param error the error to fix.
   * @return a {@link MethodRewriteChange} that represents the code fix, or {@code null} if the
   *     error cannot be fixed.
   */
  private Set<MethodRewriteChange> resolveDereferenceError(NullAwayError error) {
    if (ASTUtil.isObjectEqualsMethod(error.getRegion().member)) {
      return gpt.fixDereferenceErrorInEqualsMethod(error);
    }
    if (ASTUtil.isObjectToStringMethod(error.getRegion().member)) {
      return gpt.fixDereferenceErrorInToStringMethod(error);
    }
    if (ASTUtil.isObjectHashCodeMethod(error.getRegion().member)) {
      return gpt.fixDereferenceErrorInHashCodeMethod(error);
    }
    return Set.of();
  }

  /**
   * Applies the given {@link MethodRewriteChange} to the source code.
   *
   * @param changes the changes to apply.
   */
  public void apply(Set<MethodRewriteChange> changes) {
    changes.forEach(injector::rewriteMethod);
  }
}
