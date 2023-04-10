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

import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.checkers.Checker;
import edu.ucr.cs.riple.core.checkers.CheckerDeserializer;
import edu.ucr.cs.riple.core.checkers.nullaway.deserializers.NullAwayV3Deserializer;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.changes.AddAnnotation;
import edu.ucr.cs.riple.injector.changes.AddMarkerAnnotation;
import edu.ucr.cs.riple.injector.changes.AddSingleElementAnnotation;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.injector.location.OnParameter;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class NullAway implements Checker {

  @Override
  public CheckerDeserializer getDeserializer(Config config) {
    return new NullAwayV3Deserializer(config);
  }

  @Override
  public Set<AddAnnotation> getSuppressionAnnotations(Config config) {
    // Collect regions with remaining errors.
    Utility.buildTarget(config);
    Set<Error> remainingErrors =
        Utility.readErrorsFromOutputDirectory(config, config.targetModuleContext);
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
    // Collect @NullUnmarked annotations on classes for any remaining error.
    Utility.buildTarget(config);
    remainingErrors = Utility.readErrorsFromOutputDirectory(config, config.targetModuleContext);
    nullUnMarkedAnnotations =
        remainingErrors.stream()
            .filter(error -> !error.getRegion().isInAnonymousClass())
            .map(
                error ->
                    new AddMarkerAnnotation(
                        config.targetModuleContext.getLocationOnClass(error.getRegion().clazz),
                        config.nullUnMarkedAnnotation))
            .collect(Collectors.toSet());
    result.addAll(nullUnMarkedAnnotations);
    return result;
  }

  @Override
  public String getDefaultAnnotation() {
    return null;
  }

  @Override
  public String getCheckerName() {
    return null;
  }
}
