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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.Context;
import edu.ucr.cs.riple.core.checkers.CheckerBaseClass;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.region.Region;
import edu.ucr.cs.riple.core.module.ModuleConfiguration;
import edu.ucr.cs.riple.core.module.ModuleInfo;
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

/** Represents <a href="https://github.com/uber/NullAway">NullAway</a> checker in Annotator. */
public class NullAway extends CheckerBaseClass<NullAwayError> {

  /**
   * The name of the checker. To select this checker, this name must be used in the configurations.
   */
  public static final String NAME = "NULLAWAY";
  /** Supported version of NullAway serialization. */
  public static final int VERSION = 3;

  public NullAway(Context context) {
    super(context);
  }

  @Override
  public Set<NullAwayError> deserializeErrors(ModuleInfo module) {
    ImmutableSet<Path> paths =
        module.getModuleConfiguration().stream()
            .map(configuration -> configuration.dir.resolve("errors.tsv"))
            .collect(ImmutableSet.toImmutableSet());
    Set<NullAwayError> errors = new HashSet<>();
    paths.forEach(
        path -> {
          try {
            try (BufferedReader br = Files.newBufferedReader(path, Charset.defaultCharset())) {
              String line;
              // Skip header.
              br.readLine();
              while ((line = br.readLine()) != null) {
                errors.add(deserializeErrorFromTSVLine(module, line));
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
   * @param moduleInfo Module info.
   * @param line Given TSV line.
   * @return the deserialized error corresponding to the values in the given tsv line.
   */
  private NullAwayError deserializeErrorFromTSVLine(ModuleInfo moduleInfo, String line) {
    Context context = moduleInfo.getContext();
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
    if (nonnullTarget == null && errorType.equals(NullAwayError.METHOD_INITIALIZER_ERROR)) {
      ImmutableSet<Fix> resolvingFixes =
          generateFixesForUninitializedFields(errorMessage, region, moduleInfo);
      return createError(
          errorType,
          errorMessage,
          region,
          context.offsetHandler.getOriginalOffset(path, offset),
          resolvingFixes,
          moduleInfo);
    }
    if (nonnullTarget != null && nonnullTarget.isOnField()) {
      nonnullTarget = extendVariableList(nonnullTarget.toField(), moduleInfo);
    }
    Fix resolvingFix =
        nonnullTarget == null
            ? null
            : new Fix(
                new AddMarkerAnnotation(nonnullTarget, config.nullableAnnot), errorType, true);
    return createError(
        errorType,
        errorMessage,
        region,
        context.offsetHandler.getOriginalOffset(path, offset),
        resolvingFix == null ? ImmutableSet.of() : ImmutableSet.of(resolvingFix),
        moduleInfo);
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
              + 3
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
    if (fields.isEmpty()) {
      throw new RuntimeException(
          "Could not extract any uninitialized field in message for initializer error in version "
              + 3
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
      String errorMessage, Region region, ModuleInfo module) {
    return extractUninitializedFieldNames(errorMessage).stream()
        .map(
            field -> {
              OnField locationOnField =
                  module.getFieldRegistry().getLocationOnField(region.clazz, field);
              if (locationOnField == null) {
                return null;
              }
              return new Fix(
                  new AddMarkerAnnotation(
                      extendVariableList(locationOnField, module), config.nullableAnnot),
                  NullAwayError.METHOD_INITIALIZER_ERROR,
                  true);
            })
        .filter(Objects::nonNull)
        .collect(ImmutableSet.toImmutableSet());
  }

  /**
   * Suppresses remaining errors by following steps below:
   *
   * <ul>
   *   <li>Enclosing method of triggered errors will be marked with {@code @NullUnmarked}
   *       annotation.
   *   <li>Uninitialized fields (inline or by constructor) will be annotated as
   *       {@code @SuppressWarnings("NullAway.Init")}.
   *   <li>Explicit {@code Nullable} assignments to fields will be annotated as
   *       {@code @SuppressWarnings("NullAway")}.
   * </ul>
   */
  @Override
  public void suppressRemainingErrors() {
    throw new RuntimeException("suppressRemainingErrors Not implemented yet.");
  }

  @Override
  public void preprocess() {}

  @Override
  public NullAwayError createError(
      String errorType,
      String errorMessage,
      Region region,
      int offset,
      ImmutableSet<Fix> resolvingFixes,
      ModuleInfo module) {
    // Filter fixes on elements with explicit nonnull annotations.
    ImmutableSet<Fix> cleanedResolvingFixes =
        resolvingFixes.stream()
            .filter(f -> !module.getNonnullStore().hasExplicitNonnullAnnotation(f.toLocation()))
            .collect(ImmutableSet.toImmutableSet());
    return new NullAwayError(errorType, errorMessage, region, offset, cleanedResolvingFixes);
  }

  @Override
  public void verifyCheckerCompatibility() {
    Path pathToSerializationVersion =
        config.globalDir.resolve("0").resolve("serialization_version.txt");
    if (!Files.exists(pathToSerializationVersion)) {
      throw new RuntimeException(
          "This version of Annotator does not support the using NullAway version, please upgrade NullAway to version >= 0.10.10");
    }
    try {
      int version =
          Integer.parseInt(Files.readString(pathToSerializationVersion, Charset.defaultCharset()));
      Preconditions.checkArgument(
          version == VERSION,
          "This Annotator version only supports NullAway serialization version "
              + VERSION
              + ", but found: "
              + version
              + ", Please update Annotator or NullAway accordingly.");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void prepareConfigFilesForBuild(ImmutableSet<ModuleConfiguration> configurations) {
    configurations.forEach(
        module -> {
          FixSerializationConfig.Builder nullAwayConfig =
              new FixSerializationConfig.Builder()
                  .setSuggest(true, true)
                  .setOutputDirectory(module.dir.toString())
                  .setFieldInitInfo(true);
          nullAwayConfig.writeAsXML(module.checkerConfig.toString());
        });
  }
}
