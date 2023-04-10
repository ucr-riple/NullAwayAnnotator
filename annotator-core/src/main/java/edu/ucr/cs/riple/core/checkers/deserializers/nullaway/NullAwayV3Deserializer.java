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

package edu.ucr.cs.riple.core.checkers.deserializers.nullaway;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.Checker;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.checkers.DeserializerBaseClass;
import edu.ucr.cs.riple.core.metadata.Context;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.trackers.Region;
import edu.ucr.cs.riple.injector.Helper;
import edu.ucr.cs.riple.injector.changes.AddMarkerAnnotation;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.OnField;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * NullAway serialization adapter for version 3.
 *
 * <p>Updates to previous version (version 1):
 *
 * <ul>
 *   <li>Serialized errors contain an extra column indicating the offset of the program point where
 *       the error is reported.
 *   <li>Serialized errors contain an extra column indicating the path to the containing source file
 *       where the error is reported
 *   <li>Type arguments and Type use annotations are excluded from the serialized method signatures.
 * </ul>
 */
public class NullAwayV3Deserializer extends DeserializerBaseClass {

  public NullAwayV3Deserializer(Config config) {
    super(config, Checker.NULLAWAY, 3);
  }

  @Override
  public Set<Error> deserializeErrors(Context context) {
    ImmutableSet<Path> paths =
        context.getModules().stream()
            .map(moduleInfo -> moduleInfo.dir.resolve("errors.tsv"))
            .collect(ImmutableSet.toImmutableSet());
    Set<Error> errors = new HashSet<>();
    paths.forEach(
        path -> {
          try {
            try (BufferedReader br = Files.newBufferedReader(path, Charset.defaultCharset())) {
              String line;
              // Skip header.
              br.readLine();
              while ((line = br.readLine()) != null) {
                errors.add(deserializeErrorFromTSVLine(context, line));
              }
            }
          } catch (IOException e) {
            throw new RuntimeException("Exception happened in reading errors at: " + path, e);
          }
        });
    return errors;
  }

  /**
   * Deserializes an error from a TSV line.
   *
   * @param context the context of the module which the error is reported in.
   * @param line Given TSV line.
   * @return the deserialized error corresponding to the values in the given tsv line.
   */
  private Error deserializeErrorFromTSVLine(Context context, String line) {
    String[] values = line.split("\t");
    Preconditions.checkArgument(
        values.length == 12,
        "Expected 12 values to create Error instance in NullAway serialization version 2 but found: "
            + values.length);
    int offset = Integer.parseInt(values[4]);
    Path path = Helper.deserializePath(values[5]);
    String errorMessage = values[1];
    String errorType = values[0];
    Region region = new Region(values[2], values[3]);
    Location nonnullTarget =
        Location.createLocationFromArrayInfo(Arrays.copyOfRange(values, 6, 12));
    if (nonnullTarget == null && errorType.equals(Error.METHOD_INITIALIZER_ERROR)) {
      ImmutableSet<Fix> resolvingFixes =
          generateFixesForUninitializedFields(errorMessage, region, context).stream()
              .filter(
                  fix -> !context.getNonnullStore().hasExplicitNonnullAnnotation(fix.toLocation()))
              .collect(ImmutableSet.toImmutableSet());
      return createError(
          errorType,
          errorMessage,
          region,
          config.offsetHandler.getOriginalOffset(path, offset),
          resolvingFixes,
          context);
    }
    if (nonnullTarget != null && nonnullTarget.isOnField()) {
      nonnullTarget = extendVariableList(nonnullTarget.toField(), context);
    }
    Fix resolvingFix =
        nonnullTarget == null
            ? null
            : (context.getNonnullStore().hasExplicitNonnullAnnotation(nonnullTarget)
                // skip if element has explicit nonnull annotation.
                ? null
                : new Fix(
                    new AddMarkerAnnotation(nonnullTarget, config.nullableAnnot), errorType, true));
    return createError(
        errorType,
        errorMessage,
        region,
        config.offsetHandler.getOriginalOffset(path, offset),
        resolvingFix == null ? ImmutableSet.of() : ImmutableSet.of(resolvingFix),
        context);
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
      String errorMessage, Region region, Context context) {
    return extractUninitializedFieldNames(errorMessage).stream()
        .map(
            field -> {
              OnField locationOnField =
                  context.getFieldRegistry().getLocationOnField(region.clazz, field);
              if (locationOnField == null) {
                return null;
              }
              return new Fix(
                  new AddMarkerAnnotation(
                      extendVariableList(locationOnField, context), config.nullableAnnot),
                  Error.METHOD_INITIALIZER_ERROR,
                  true);
            })
        .filter(Objects::nonNull)
        .collect(ImmutableSet.toImmutableSet());
  }
}
