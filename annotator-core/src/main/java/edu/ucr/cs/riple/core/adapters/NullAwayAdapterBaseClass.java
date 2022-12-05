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
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/** Base class for all NullAway serialization adapters. */
public abstract class NullAwayAdapterBaseClass implements NullAwayVersionAdapter {

  /** Annotator config. */
  protected final Config config;
  /** Serialization version. */
  protected final int version;
  /**
   * Field declaration store instance, used to generate fixes for uninitialized fields based on
   * error message for initializers.
   */
  protected final FieldDeclarationStore fieldDeclarationStore;

  public NullAwayAdapterBaseClass(
      Config config, FieldDeclarationStore fieldDeclarationStore, int version) {
    this.config = config;
    this.version = version;
    this.fieldDeclarationStore = fieldDeclarationStore;
  }

  /**
   * Extracts uninitialized field names from the given error message.
   *
   * @param errorMessage Error message.
   * @return Set of uninitialized field names.
   */
  private Set<String> extractUnInitializedFields(String errorMessage) {
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
              + version
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
              + version
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
  protected Set<Fix> generateFixForUnInitializedFields(String errorMessage, Region region) {
    return extractUnInitializedFields(errorMessage).stream()
        .map(
            field ->
                new Fix(
                    new AddMarkerAnnotation(
                        fieldDeclarationStore.getLocationOnField(region.clazz, field),
                        config.nullableAnnot),
                    "METHOD_NO_INIT",
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
   * @param nonnullTarget If {@code @Nonnull}, this error involved a pseudo-assignment of
   *     a @Nullable expression into a @NonNull target, and this field is the Symbol for that
   *     target.
   * @return The corresponding error.
   */
  protected Error createError(
      String errorType, String errorMessage, Region region, @Nullable Location nonnullTarget) {
    if (nonnullTarget == null && errorType.equals("METHOD_NO_INIT")) {
      Set<Fix> resolvingFix = generateFixForUnInitializedFields(errorMessage, region);
      return new Error(errorType, errorMessage, region, resolvingFix);
    }
    Fix resolvingFix =
        nonnullTarget == null
            ? null
            : new Fix(
                new AddMarkerAnnotation(nonnullTarget, config.nullableAnnot),
                errorType,
                region,
                true);
    return new Error(errorType, errorMessage, region, resolvingFix);
  }
}
