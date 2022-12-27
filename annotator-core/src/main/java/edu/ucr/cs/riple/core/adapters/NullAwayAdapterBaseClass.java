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

package edu.ucr.cs.riple.core.adapters;

import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.metadata.field.FieldDeclarationStore;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.trackers.Region;
import edu.ucr.cs.riple.injector.changes.AddMarkerAnnotation;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.OnField;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/** Base class for all NullAway serialization adapters. */
public abstract class NullAwayAdapterBaseClass implements NullAwayVersionAdapter {

  /** Annotator config. */
  protected final Config config;

  /**
   * Field declaration store instance, used to generate fixes for uninitialized fields based on
   * error message for initializers.
   */
  protected final FieldDeclarationStore fieldDeclarationStore;

  public NullAwayAdapterBaseClass(Config config, FieldDeclarationStore fieldDeclarationStore) {
    this.config = config;
    this.fieldDeclarationStore = fieldDeclarationStore;
  }

  /**
   * Extracts uninitialized field names from the given error message.
   *
   * @param errorMessage Error message.
   * @return Set of uninitialized field names.
   */
  private Set<String> extractUninitializedFieldNames(String errorMessage) {
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
              + getVersionNumber()
              + ": "
              + errorMessage);
    }
    String[] fieldsData = errorMessage.substring(begin, end).split(",");
    Set<String> fields =
        Arrays.stream(fieldsData)
            .map(s -> s.substring(0, s.indexOf("(")).trim())
            .collect(Collectors.toSet());
    if (fields.size() == 0) {
      throw new RuntimeException(
          "Could not extract any uninitialized field in message for initializer error in version "
              + getVersionNumber()
              + ": "
              + errorMessage);
    }
    return fields;
  }

  /**
   * Generates a set of fixes for uninitialized fields from the given error message.
   *
   * @param errorMessage Given error message.
   * @param region Region where the error is reported.
   * @return Set of fixes for uninitialized fields to resolve the given error.
   */
  protected Set<Fix> generateFixesForUninitializedFields(
      String errorMessage, Region region, FieldDeclarationStore store) {
    return extractUninitializedFieldNames(errorMessage).stream()
        .map(
            field ->
                new Fix(
                    new AddMarkerAnnotation(
                        extendVariableList(store.getLocationOnField(region.clazz, field), store),
                        config.nullableAnnot),
                    Error.METHOD_INITIALIZER_ERROR,
                    region,
                    true))
        .collect(Collectors.toSet());
  }

  /**
   * Creates an {@link Error} instance.
   *
   * @param errorType Error type.
   * @param errorMessage Error message.
   * @param region Region where the error is reported,
   * @param offset offset of program point in original version where error is reported.
   * @param nonnullTarget If {@code @Nonnull}, this error involved a pseudo-assignment of
   *     a @Nullable expression into a @NonNull target, and this field is the Symbol for that
   *     target.
   * @return The corresponding error.
   */
  protected Error createError(
      String errorType,
      String errorMessage,
      Region region,
      int offset,
      @Nullable Location nonnullTarget,
      FieldDeclarationStore store) {
    if (nonnullTarget == null && errorType.equals(Error.METHOD_INITIALIZER_ERROR)) {
      Set<Fix> resolvingFixes = generateFixesForUninitializedFields(errorMessage, region, store);
      return new Error(errorType, errorMessage, region, offset, resolvingFixes);
    }
    if (nonnullTarget != null && nonnullTarget.isOnField()) {
      nonnullTarget = extendVariableList(nonnullTarget.toField(), store);
    }
    Fix resolvingFix =
        nonnullTarget == null
            ? null
            : new Fix(
                new AddMarkerAnnotation(nonnullTarget, config.nullableAnnot),
                errorType,
                region,
                true);
    return new Error(errorType, errorMessage, region, offset, resolvingFix);
  }

  /**
   * Extends field variable names to full list to include all variables declared in the same
   * statement.
   *
   * @param onField Location of the field.
   * @param store Field Declaration Store instance.
   * @return The updated given location.
   */
  private static OnField extendVariableList(OnField onField, FieldDeclarationStore store) {
    Set<String> variables =
        store.getInLineMultipleFieldDeclarationsOnField(onField.clazz, onField.variables);
    onField.variables.addAll(variables);
    return onField;
  }
}
