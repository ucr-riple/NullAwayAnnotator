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
import edu.ucr.cs.riple.core.checkers.DiagnosticPosition;
import edu.ucr.cs.riple.core.registries.index.Error;
import edu.ucr.cs.riple.core.registries.index.Fix;
import edu.ucr.cs.riple.core.registries.region.Region;
import edu.ucr.cs.riple.injector.changes.AddAnnotation;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Represents an error reported by {@link NullAway}. */
public class NullAwayError extends Error {

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
      DiagnosticPosition position,
      Set<AddAnnotation> annotations) {
    super(messageType, message, region, path, position, annotations);
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
        && position.equals(error.position);
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
        position);
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
   * Extracts the placeholder value from the error message.
   *
   * @param error the error to extract the placeholder value from.
   * @return the placeholder value.
   */
  public static String[] extractPlaceHolderValue(NullAwayError error) {
    switch (error.messageType) {
      case "DEREFERENCE_NULLABLE":
        {
          final Pattern pattern =
              Pattern.compile(
                  "(?:dereferenced expression|enhanced-for expression) (.+?) is @Nullable --- (.+?) --- (.+?) --- (.+?) --- (.+?) --- (.+)");
          Matcher matcher = pattern.matcher(error.message);
          if (matcher.find()) {
            return new String[] {
              // expression
              matcher.group(1),
              // type
              matcher.group(2),
              // enclosing class
              matcher.group(3),
              // annotated?
              matcher.group(4),
              // deferred symbol
              matcher.group(5),
              // expression start position
              matcher.group(6)
            };
          }
          break;
        }
      case "METHOD_NO_INIT":
        String errorMessage = error.message;
        String prefix = "initializer method does not guarantee @NonNull field";
        int begin = prefix.length();
        if (errorMessage.charAt(begin) == 's') {
          begin += 1;
        }
        int end = errorMessage.indexOf(" is initialized along");
        end = end == -1 ? errorMessage.indexOf(" are initialized along ") : end;
        if (end == -1) {
          throw new RuntimeException(
              "Error message for initializer error not recognized in version "
                  + 3
                  + ": "
                  + errorMessage);
        }
        String[] fieldsData = errorMessage.substring(begin, end).split(",");
        return Arrays.stream(fieldsData)
            .map(s -> s.substring(0, s.indexOf("(")).trim())
            .distinct()
            .toArray(String[]::new);
      case "FIELD_NO_INIT":
        {
          Pattern pattern = Pattern.compile("@NonNull field (\\w+) not initialized");
          Matcher matcher = pattern.matcher(error.message);
          if (matcher.find()) {
            return new String[] {matcher.group(1)};
          }
          break;
        }
    }
    throw new IllegalArgumentException(
        "Error type not supported." + error.messageType + ": " + error.message);
  }
}
