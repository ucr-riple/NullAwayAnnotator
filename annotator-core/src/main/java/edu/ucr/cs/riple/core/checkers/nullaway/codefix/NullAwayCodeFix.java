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

public class NullAwayCodeFix {

  private final ChatGPT gpt;
  private final Injector injector;
  private final Config config;

  public NullAwayCodeFix(Config config) {
    this.gpt = new ChatGPT(config);
    this.injector = new Injector(config.languageLevel);
    this.config = config;
  }

  public MethodRewriteChange fix(NullAwayError error) {
    switch (error.messageType) {
      case "DEREFERENCE_NULLABLE":
        return resolveDereferenceError(error);
      default:
        return null;
    }
  }

  private MethodRewriteChange resolveDereferenceError(NullAwayError error) {
    if (ASTUtil.isObjectEqualsMethod(error.getRegion().member)) {
      return gpt.fixDereferenceErrorInEqualsMethod(error);
    }
    return null;
  }

  public void apply(MethodRewriteChange methodRewriteChange) {
    injector.rewriteMethod(methodRewriteChange);
  }
}
