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

import edu.ucr.cs.riple.core.Context;
import edu.ucr.cs.riple.core.checkers.nullaway.NullAway;
import edu.ucr.cs.riple.core.checkers.nullaway.NullAwayError;
import edu.ucr.cs.riple.core.checkers.nullaway.codefix.agent.ChatGPT;
import edu.ucr.cs.riple.core.util.ASTUtil;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.Injector;
import edu.ucr.cs.riple.injector.SourceCode;
import edu.ucr.cs.riple.injector.changes.MethodRewriteChange;
import edu.ucr.cs.riple.injector.location.OnMethod;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A class that provides code fixes for {@link NullAwayError}s. */
public class NullAwayCodeFix {

  /** The {@link ChatGPT} instance used to generate code fixes. */
  private final ChatGPT gpt;

  /** The {@link Injector} instance used to apply code fixes. */
  private final Injector injector;

  /** Annotation context instance. */
  private final Context context;

  public NullAwayCodeFix(Context context) {
    this.gpt = new ChatGPT(context);
    this.context = context;
    this.injector = new Injector(context.config.languageLevel);
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
    // Check if it is a false positive
    if (gpt.checkIfFalsePositiveAtErrorPoint(error)) {
      // cast to nonnull.
      return constructCastToNonNullMethodRewriteForError(error);
    }
    return Set.of();
  }

  /**
   * Constructs a {@link MethodRewriteChange} that casts the dereferenced nullable value shown in
   * the error to non-null.
   *
   * @param error the error to fix.
   * @return a {@link MethodRewriteChange} that represents the code fix.
   */
  private Set<MethodRewriteChange> constructCastToNonNullMethodRewriteForError(
      NullAwayError error) {
    SourceCode enclosingMethod =
        ASTUtil.getRegionSourceCode(context.config, error.path, error.getRegion());
    if (enclosingMethod == null) {
      return Set.of();
    }
    String[] lines = enclosingMethod.content.split("\n");
    final Pattern pattern = Pattern.compile("dereferenced expression (\\w+) is @Nullable");
    Matcher matcher = pattern.matcher(error.message);
    if (!matcher.find()) {
      return Set.of();
    }
    // calculate the erroneous line in method. We have to adjust the line number to the method's
    // range. Note that the line number is 1-based in java parser, and we need to adjust it to
    // 0-based.
    int errorLine = error.position.lineNumber - (enclosingMethod.range.begin.line - 1);
    String expression = matcher.group(1);
    String preconditionStatement =
        String.format(
            "%sPreconditions.checkArgument(%s != null, \"expected %s to be nonnull here.\");\n",
            Utility.getLeadingWhitespace(lines[errorLine]), expression, expression);
    lines[errorLine] = preconditionStatement + lines[errorLine];
    MethodRewriteChange change =
        new MethodRewriteChange(
            new OnMethod(error.path, error.getRegion().clazz, error.getRegion().member),
            String.join("\n", lines),
            // Add the import required for Preconditions.
            Set.of(NullAway.PRECONDITION_NAME));
    return Set.of(change);
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
