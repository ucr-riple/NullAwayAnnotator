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

import edu.ucr.cs.riple.core.registries.index.Error;
import edu.ucr.cs.riple.core.registries.index.Fix;
import edu.ucr.cs.riple.core.registries.region.Region;
import edu.ucr.cs.riple.injector.changes.AddAnnotation;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Represents an error reported by {@link NullAway}. */
public class NullAwayError extends Error {

  /** Error type for method initialization errors from NullAway in {@code String}. */
  public static final String METHOD_INITIALIZER_ERROR = "METHOD_NO_INIT";
  /** Error type for field initialization errors from NullAway in {@code String}. */
  public static final String FIELD_INITIALIZER_ERROR = "FIELD_NO_INIT";

  public NullAwayError(
      String messageType,
      String message,
      Region region,
      int offset,
      Set<AddAnnotation> annotations) {
    super(messageType, message, region, offset, annotations);
  }

  @Override
  protected Set<Fix> computeFixesFromAnnotations(Set<AddAnnotation> annotations) {
    // In NullAway inference, each annotation is examined individually. Thus, we create a separate
    // fix instance for each annotation.
    return annotations.stream().map(Fix::new).collect(Collectors.toSet());
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
    if (messageType.equals(METHOD_INITIALIZER_ERROR)) {
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
        messageType.equals(METHOD_INITIALIZER_ERROR) ? METHOD_INITIALIZER_ERROR : message,
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
    return !this.messageType.equals(METHOD_INITIALIZER_ERROR)
        && !this.messageType.equals(FIELD_INITIALIZER_ERROR);
  }
}
