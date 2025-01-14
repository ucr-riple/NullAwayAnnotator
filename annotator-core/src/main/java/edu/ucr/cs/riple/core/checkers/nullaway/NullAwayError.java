/*
 * MIT License
 *
 * Copyright (c) 2023 Nima Karimipour
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

package edu.ucr.cs.riple.core.checkers.nullaway;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.registries.index.Error;
import edu.ucr.cs.riple.core.registries.index.Fix;
import edu.ucr.cs.riple.core.registries.region.Region;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.changes.AddAnnotation;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Represents an error reported by {@link NullAway}. */
public class NullAwayError extends Error {

  /** The line of source code associated with the diagnostic offset. */
  public final String diagnosticLine;

  public enum ErrorType {
    METHOD_INITIALIZER("METHOD_NO_INIT"),
    FIELD_INITIALIZER("FIELD_NO_INIT"),
    DEREFERENCE("DEREFERENCE");

    public final String type;

    ErrorType(String type) {
      this.type = type;
    }
  }

  public NullAwayError(
      String messageType,
      String message,
      Region region,
      Path path,
      int offset,
      Set<AddAnnotation> annotations) {
    super(messageType, message, region, path, offset, annotations);
    this.diagnosticLine = getDiagnosticSourceLine(path, offset);
  }

  @Override
  protected ImmutableSet<Fix> computeFixesFromAnnotations(Set<AddAnnotation> annotations) {
    // In NullAway inference, each annotation is examined individually. Thus, we create a separate
    // fix instance for each annotation.
    return annotations.stream().map(Fix::new).collect(ImmutableSet.toImmutableSet());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NullAwayError)) {
      return false;
    }
    NullAwayError error = (NullAwayError) o;
    if (!messageType.equals(error.messageType)) {
      return false;
    }
    if (!region.equals(error.region)) {
      return false;
    }
    if (messageType.equals(ErrorType.METHOD_INITIALIZER.type)) {
      // we do not need to compare error messages as it can be the same error with a different error
      // message and should not be treated as a separate error.
      return true;
    }
    return message.equals(error.message)
        && resolvingFixes.equals(error.resolvingFixes)
        && offset == error.offset;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        messageType,
        // to make sure equal objects will produce the same hashcode.
        messageType.equals(ErrorType.FIELD_INITIALIZER.type)
            ? ErrorType.METHOD_INITIALIZER.type
            : message,
        region,
        resolvingFixes,
        offset);
  }

  /**
   * Returns true if the error is an initialization error ({@code METHOD_NO_INIT} or {@code
   * FIELD_NO_INIT}).
   *
   * @return true, if the error is an initialization error.
   */
  public boolean isNonInitializationError() {
    return !this.messageType.equals(ErrorType.METHOD_INITIALIZER.type)
        && !this.messageType.equals(ErrorType.FIELD_INITIALIZER.type);
  }

  /**
   * Retrieves the specific line of source code associated with the given diagnostic offset.
   *
   * @param path the path to the source file.
   * @param offset the character offset within the source file.
   * @return the source code line corresponding to the diagnostics location.
   */
  private String getDiagnosticSourceLine(Path path, int offset) {
    List<String> content = Utility.readFileLines(path);
    int index = 0;
    for (String line : content) {
      if (index + line.length() >= offset) {
        return line.trim();
      }
      index += line.length();
    }
    return "";
  }
}
