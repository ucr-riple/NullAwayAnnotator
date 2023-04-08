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

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.metadata.field.FieldRegistry;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.index.NonnullStore;
import edu.ucr.cs.riple.core.metadata.trackers.Region;
import edu.ucr.cs.riple.injector.changes.AddMarkerAnnotation;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.OnField;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/** Base class for all NullAway serialization adapters. */
public abstract class NullAwayAdapterBaseClass implements NullAwayVersionAdapter {

  /** Annotator config. */
  protected final Config config;

  /**
   * Field registry instance, used to generate fixes for uninitialized fields based on error message
   * for initializers.
   */
  protected final FieldRegistry fieldRegistry;

  /**
   * Nonnull store used to prevent Annotator from generating fixes for elements with explicit
   * {@code @Nonnull} annotations.
   */
  private final NonnullStore nonnullStore;

  public NullAwayAdapterBaseClass(
      Config config, FieldRegistry fieldRegistry, NonnullStore nonnullStore) {
    this.config = config;
    this.fieldRegistry = fieldRegistry;
    this.nonnullStore = nonnullStore;
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
            // NullAway serializes line number right after a field name starting with an open
            // parentheses. (e.g. foo (line z)). This approach of extracting field names is
            // extremely dependent on the format of NullAway error messages. Should be watched
            // carefully and updated if the format is changed by NullAway (maybe regex?).
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
  protected ImmutableSet<Fix> generateFixesForUninitializedFields(
      String errorMessage, Region region, FieldRegistry registry) {
    return extractUninitializedFieldNames(errorMessage).stream()
        .map(
            field -> {
              OnField locationOnField = registry.getLocationOnField(region.clazz, field);
              if (locationOnField == null) {
                return null;
              }
              return new Fix(
                  new AddMarkerAnnotation(
                      extendVariableList(locationOnField, registry), config.nullableAnnot),
                  Error.METHOD_INITIALIZER_ERROR,
                  true);
            })
        .filter(Objects::nonNull)
        .collect(ImmutableSet.toImmutableSet());
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
      FieldRegistry registry) {
    if (nonnullTarget == null && errorType.equals(Error.METHOD_INITIALIZER_ERROR)) {
      ImmutableSet<Fix> resolvingFixes =
          generateFixesForUninitializedFields(errorMessage, region, registry).stream()
              .filter(fix -> !nonnullStore.hasExplicitNonnullAnnotation(fix.toLocation()))
              .collect(ImmutableSet.toImmutableSet());
      return new Error(errorType, errorMessage, region, offset, resolvingFixes);
    }
    if (nonnullTarget != null && nonnullTarget.isOnField()) {
      nonnullTarget = extendVariableList(nonnullTarget.toField(), registry);
    }
    Fix resolvingFix =
        nonnullTarget == null
            ? null
            : (nonnullStore.hasExplicitNonnullAnnotation(nonnullTarget)
                // skip if element has explicit nonnull annotation.
                ? null
                : new Fix(
                    new AddMarkerAnnotation(nonnullTarget, config.nullableAnnot), errorType, true));
    return new Error(errorType, errorMessage, region, offset, resolvingFix);
  }

  /**
   * Extends field variable names to full list to include all variables declared in the same
   * statement.
   *
   * @param onField Location of the field.
   * @param registry Field registry instance.
   * @return The updated given location.
   */
  private static OnField extendVariableList(OnField onField, FieldRegistry registry) {
    Set<String> variables =
        registry.getInLineMultipleFieldDeclarationsOnField(onField.clazz, onField.variables);
    onField.variables.addAll(variables);
    return onField;
  }
}
