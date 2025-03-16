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
import com.google.gson.JsonObject;
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

  /** Additional information about the error. */
  private final JsonObject infos;

  public enum ErrorType {
    METHOD_INITIALIZER("METHOD_NO_INIT"),
    FIELD_INITIALIZER("FIELD_NO_INIT"),
    DEREFERENCE("DEREFERENCE");

    public final String type;

    ErrorType(String type) {
      this.type = type;
    }
  }

  /** Contains information about the nullable expression causing the error. */
  public static class NullableExpressionInfo {
    /** The expression that is nullable. */
    public final String expression;

    /** Whether the expression is in annotated packages. */
    public final boolean isAnnotated;

    /** Kind of the expression. */
    public final String kind;

    /** Enclosing class of the expression. */
    public final String clazz;

    /** Position of the expression. */
    public final int position;

    /** Symbol of the expression. */
    public final String symbol;

    public NullableExpressionInfo(JsonObject obj) {
      this.expression = obj.get("expression").getAsString();
      this.isAnnotated = obj.get("isAnnotated").getAsBoolean();
      this.kind = obj.get("kind").getAsString();
      this.clazz = obj.get("class").getAsString();
      this.position = obj.get("position").getAsInt();
      this.symbol = obj.get("symbol").getAsString();
    }
  }

  public NullAwayError(
      String messageType,
      String message,
      Region region,
      Path path,
      DiagnosticPosition position,
      JsonObject infos,
      Set<AddAnnotation> annotations) {
    super(messageType, message, region, path, position, annotations);
    this.infos = infos;
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
   * Returns the nullable expression information.
   *
   * @return the nullable expression information.
   */
  public NullableExpressionInfo getNullableExpressionInfo() {
    return new NullableExpressionInfo(infos);
  }

  /**
   * Returns the nullable expression causing the error.
   *
   * @return the nullable expression.
   */
  public String getNullableExpression() {
    switch (messageType) {
      case "DEREFERENCE_NULLABLE":
        return getNullableExpressionInfo().expression;
      case "PASS_NULLABLE":
        Pattern pattern = Pattern.compile("@NonNull field (\\w+) not initialized");
        Matcher matcher = pattern.matcher(message);
        return matcher.group(1);
      default:
        throw new IllegalArgumentException(
            "Error type not supported to extract nullable expression from: "
                + messageType
                + ": "
                + message);
    }
  }

  /**
   * Returns the uninitialized fields from the error message if the error is an initialization
   * error.
   *
   * @return the uninitialized fields.
   */
  public String[] getUninitializedFieldsFromErrorMessage() {
    switch (messageType) {
      case "METHOD_NO_INIT":
        String errorMessage = message;
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
          Matcher matcher = pattern.matcher(message);
          return new String[] {matcher.group(1)};
        }
      default:
        throw new IllegalArgumentException(
            "Error type not supported to extract uninitialized fields from: "
                + messageType
                + ": "
                + message);
    }
  }
}
