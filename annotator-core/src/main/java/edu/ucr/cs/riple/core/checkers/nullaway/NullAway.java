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

package edu.ucr.cs.riple.core.checkers.nullaway;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.checkers.CheckerBaseClass;
import edu.ucr.cs.riple.core.injectors.AnnotationInjector;
import edu.ucr.cs.riple.core.metadata.Context;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.trackers.Region;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.Helper;
import edu.ucr.cs.riple.injector.changes.AddAnnotation;
import edu.ucr.cs.riple.injector.changes.AddMarkerAnnotation;
import edu.ucr.cs.riple.injector.changes.AddSingleElementAnnotation;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.injector.location.OnParameter;
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

public class NullAway extends CheckerBaseClass<NullAwayError> {

  public static final String NAME = "NULLAWAY";

  public NullAway(Config config) {
    super(config, 3);
  }

  @Override
  public Set<NullAwayError> deserializeErrors(Context context) {
    ImmutableSet<Path> paths =
        context.getModules().stream()
            .map(moduleInfo -> moduleInfo.dir.resolve("errors.tsv"))
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
  private NullAwayError deserializeErrorFromTSVLine(Context context, String line) {
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
    if (fields.size() == 0) {
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
                  NullAwayError.METHOD_INITIALIZER_ERROR,
                  true);
            })
        .filter(Objects::nonNull)
        .collect(ImmutableSet.toImmutableSet());
  }

  @Override
  public void suppressRemainingAnnotations(Config config, AnnotationInjector injector) {
    // Collect regions with remaining errors.
    Utility.buildTarget(config);
    Set<NullAwayError> remainingErrors = deserializeErrors(config.targetModuleContext);
    Set<Fix> remainingFixes =
        Utility.readFixesFromOutputDirectory(config, config.targetModuleContext);
    // Collect all regions for NullUnmarked.
    // For all errors in regions which correspond to a method's body, we can add @NullUnmarked at
    // the method level.
    Set<AddAnnotation> nullUnMarkedAnnotations =
        remainingErrors.stream()
            // find the corresponding method nodes.
            .map(
                error -> {
                  if (error.getRegion().isOnCallable()
                      &&
                      // We suppress initialization errors reported on constructors using
                      // @SuppressWarnings("NullAway.Init"). We add @NullUnmarked on constructors
                      // only for errors in the body of the constructor.
                      !error.isInitializationError()) {
                    return config
                        .targetModuleContext
                        .getMethodRegistry()
                        .findMethodByName(error.encClass(), error.encMember());
                  }
                  // For methods invoked in an initialization region, where the error is that
                  // `@Nullable` is being passed as an argument, we add a `@NullUnmarked` annotation
                  // to the called method.
                  if (error.messageType.equals("PASS_NULLABLE")
                      && error.isSingleFix()
                      && error.toResolvingLocation().isOnParameter()) {
                    OnParameter nullableParameter = error.toResolvingParameter();
                    return config
                        .targetModuleContext
                        .getMethodRegistry()
                        .findMethodByName(nullableParameter.clazz, nullableParameter.method);
                  }
                  return null;
                })
            // Filter null values from map above.
            .filter(Objects::nonNull)
            .map(node -> new AddMarkerAnnotation(node.location, config.nullUnMarkedAnnotation))
            .collect(Collectors.toSet());

    // For errors within static initialization blocks, add a @NullUnmarked annotation on the
    // enclosing class
    nullUnMarkedAnnotations.addAll(
        remainingErrors.stream()
            .filter(
                error ->
                    error.getRegion().isOnInitializationBlock()
                        && !error.getRegion().isInAnonymousClass())
            .map(
                error ->
                    new AddMarkerAnnotation(
                        config.targetModuleContext.getLocationOnClass(error.getRegion().clazz),
                        config.nullUnMarkedAnnotation))
            .collect(Collectors.toSet()));
    Set<AddAnnotation> result = new HashSet<>(nullUnMarkedAnnotations);

    // Collect suppress warnings, errors on field declaration regions.
    Set<OnField> fieldsWithSuppressWarnings =
        remainingErrors.stream()
            .filter(
                error -> {
                  if (!error.getRegion().isOnField()) {
                    return false;
                  }
                  if (error.messageType.equals("PASS_NULLABLE")) {
                    // It is already resolved with @NullUnmarked selected above.
                    return false;
                  }
                  // We can silence them by SuppressWarnings("NullAway.Init")
                  return !error.isInitializationError();
                })
            .map(
                error ->
                    config
                        .targetModuleContext
                        .getFieldRegistry()
                        .getLocationOnField(error.getRegion().clazz, error.getRegion().member))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Set<AddAnnotation> suppressWarningsAnnotations =
        fieldsWithSuppressWarnings.stream()
            .map(
                onField ->
                    new AddSingleElementAnnotation(onField, "SuppressWarnings", "NullAway", false))
            .collect(Collectors.toSet());
    result.addAll(suppressWarningsAnnotations);

    // Collect NullAway.Init SuppressWarnings
    Set<AddAnnotation> initializationSuppressWarningsAnnotations =
        remainingFixes.stream()
            .filter(
                fix ->
                    fix.isOnField()
                        && (fix.reasons.contains("METHOD_NO_INIT")
                            || fix.reasons.contains("FIELD_NO_INIT")))
            // Filter nodes annotated with SuppressWarnings("NullAway")
            .filter(fix -> !fieldsWithSuppressWarnings.contains(fix.toField()))
            .map(
                fix ->
                    new AddSingleElementAnnotation(
                        fix.toField(), "SuppressWarnings", "NullAway.Init", false))
            .collect(Collectors.toSet());
    result.addAll(initializationSuppressWarningsAnnotations);
    injector.injectAnnotations(result);
    // update log
    config.log.updateInjectedAnnotations(result);
    // Collect @NullUnmarked annotations on classes for any remaining error.
    Utility.buildTarget(config);
    remainingErrors = deserializeErrors(config.targetModuleContext);
    nullUnMarkedAnnotations =
        remainingErrors.stream()
            .filter(error -> !error.getRegion().isInAnonymousClass())
            .map(
                error ->
                    new AddMarkerAnnotation(
                        config.targetModuleContext.getLocationOnClass(error.getRegion().clazz),
                        config.nullUnMarkedAnnotation))
            .collect(Collectors.toSet());
    injector.injectAnnotations(nullUnMarkedAnnotations);
    // update log
    config.log.updateInjectedAnnotations(nullUnMarkedAnnotations);
  }

  @Override
  public String getDefaultAnnotation() {
    return null;
  }

  @Override
  public String getCheckerName() {
    return null;
  }

  @Override
  public NullAwayError createErrorFactory(
      String errorType, String errorMessage, Region region, int offset, Set<Fix> resolvingFixes) {
    return null;
  }

  @Override
  public int getVersion() {
    return 0;
  }
}
