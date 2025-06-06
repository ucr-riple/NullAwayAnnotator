/*
 * MIT License
 *
 * Copyright (c) 2025 Nima Karimipour
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

package edu.ucr.cs.riple.core.checkers.nullaway.codefix;

import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import edu.ucr.cs.riple.core.Context;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.cache.TargetModuleCache;
import edu.ucr.cs.riple.core.cache.downstream.VoidDownstreamImpactCache;
import edu.ucr.cs.riple.core.checkers.nullaway.NullAwayError;
import edu.ucr.cs.riple.core.evaluators.BasicEvaluator;
import edu.ucr.cs.riple.core.evaluators.Evaluator;
import edu.ucr.cs.riple.core.evaluators.suppliers.TargetModuleSupplier;
import edu.ucr.cs.riple.core.registries.index.Error;
import edu.ucr.cs.riple.core.registries.index.Fix;
import edu.ucr.cs.riple.core.registries.method.MethodRecord;
import edu.ucr.cs.riple.core.registries.method.invocation.InvocationRecord;
import edu.ucr.cs.riple.core.registries.method.invocation.InvocationRecordRegistry;
import edu.ucr.cs.riple.core.registries.region.Region;
import edu.ucr.cs.riple.core.util.ASTParser;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.SourceCode;
import edu.ucr.cs.riple.injector.changes.AddAnnotation;
import edu.ucr.cs.riple.injector.changes.AddMarkerAnnotation;
import edu.ucr.cs.riple.injector.changes.AddSingleElementAnnotation;
import edu.ucr.cs.riple.injector.changes.MethodRewriteChange;
import edu.ucr.cs.riple.injector.changes.RegionRewrite;
import edu.ucr.cs.riple.injector.changes.RemoveAnnotation;
import edu.ucr.cs.riple.injector.changes.RemoveMarkerAnnotation;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.injector.location.OnMethod;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class that provides code fixes for {@link NullAwayError}s. This models uses an agentic
 * structured prompting to resolve errors.
 */
public class AdvancedNullAwayCodeFix extends NullAwayCodeFix {

  /** Invocation record registry to retrieve the callers of a method. */
  private final InvocationRecordRegistry invocationRecordRegistry;

  /** Basic nullaway code fix to handle unknown cases. */
  private final BasicNullAwayCodeFix basicNullAwayCodeFix;

  /** The logger instance. */
  private final Logger logger;

  /**
   * Simply returns an empty set, meaning no action is needed. The purpose is only increasing
   * readability.
   */
  private static final Set<RegionRewrite> NO_ACTION = Set.of();

  public AdvancedNullAwayCodeFix(Context context) {
    super(context);
    this.invocationRecordRegistry = new InvocationRecordRegistry(context.targetModuleInfo, parser);
    this.basicNullAwayCodeFix = new BasicNullAwayCodeFix(context);
    this.logger = LoggerFactory.getLogger(AdvancedNullAwayCodeFix.class);
  }

  /**
   * Generates a code fix for the given {@link NullAwayError}. The fix is rewrites of sets of
   * methods.
   *
   * @param error the error to fix.
   * @return a {@link MethodRewriteChange} that represents the code fix, or {@code null} if the
   *     error cannot be fixed.
   */
  @Override
  public Set<RegionRewrite> fix(NullAwayError error) {
    logger.trace("Fixing error: {}", error);
    switch (error.messageType) {
      case "DEREFERENCE_NULLABLE":
        return resolveDereferenceError(error);
      case "FIELD_NO_INIT":
      case "METHOD_NO_INIT":
        return resolveUninitializedField(error);
      case "ASSIGN_FIELD_NULLABLE":
        return resolveAssignFieldNullableError(error);
      case "RETURN_NULLABLE":
        return resolveNullableReturnError(error);
      case "WRONG_OVERRIDE_RETURN":
        return resolveWrongOverrideReturnError(error);
      default:
        logger.trace("Error type not recognized: {}", error.messageType);
        return NO_ACTION;
        //        throw new IllegalStateException("Unknown error type: " + error.messageType);
    }
  }

  /**
   * Resolves a wrong override return error by generating a code fix. Currently, the only solution
   * we make the super method nullable and resolve triggered errors.
   *
   * @param error the error to fix.
   * @return a {@link MethodRewriteChange} that represents the code fix, or {@code NO_ACTION} if the
   *     error cannot be fixed.
   */
  private Set<RegionRewrite> resolveWrongOverrideReturnError(NullAwayError error) {
    logger.trace("Fixing wrong override return error.");
    // make super method nullable.
    OnMethod methodLocation =
        context
            .targetModuleInfo
            .getMethodRegistry()
            .findMethodByName(error.getRegion().clazz, error.getRegion().member)
            .location;
    MethodRecord superMethod =
        context.targetModuleInfo.getMethodRegistry().getImmediateSuperMethod(methodLocation);
    if (superMethod == null) {
      return NO_ACTION;
    }
    logger.trace("Making the super method nullable.");
    // add annotation to super method
    context.injector.injectAnnotation(
        new AddMarkerAnnotation(superMethod.location, context.config.nullableAnnot));
    // resolve triggered errors.
    logger.trace("Resolving triggered errors for making super method nullable.");
    return fixTriggeredErrorsForLocation(methodLocation);
  }

  /**
   * Resolves an assign field nullable error by generating a code fix. Currently, the only solution
   * we follow is making the field {@code @Nullable} and resolving triggered errors.
   *
   * @param error the error to fix.
   * @return a {@link MethodRewriteChange} that represents the code fix, or {@code null} if the
   */
  private Set<RegionRewrite> resolveAssignFieldNullableError(NullAwayError error) {
    logger.trace("Resolving assign field nullable error.");
    // currently the only solution we follow is to make the field nullable and resolve triggered
    // errors.
    Preconditions.checkArgument(error.getResolvingFixes().size() == 1);
    // Make the field nullable.
    Fix fix = error.getResolvingFixes().iterator().next();
    logger.trace("Making the field nullable.");
    context.injector.injectFixes(Set.of(fix));
    ImmutableSet<Error> triggeredErrors = getTriggeredErrorsFromError(error);
    if (triggeredErrors == null) {
      return NO_ACTION;
    }
    // Add any annotations that triggered errors contain:
    logger.trace("Adding all triggered annotations.");
    Set<Fix> fixes =
        triggeredErrors.stream()
            .map(Error::getResolvingFixes)
            .flatMap(Set::stream)
            .collect(ImmutableSet.toImmutableSet());
    context.injector.injectFixes(fixes);
    // Resolve the ones where annotation cannot fix
    Set<NullAwayError> unresolvableErrors =
        triggeredErrors.stream()
            .filter(e -> e.getResolvingFixes().isEmpty())
            .map(e -> (NullAwayError) e)
            .collect(Collectors.toSet());
    Set<RegionRewrite> changes = new HashSet<>();
    logger.trace("Resolving unresolvable errors.");
    for (NullAwayError unresolvableError : unresolvableErrors) {
      logger.trace("Resolving unresolvable error: {}", unresolvableError);
      Set<RegionRewrite> change = fix(unresolvableError);
      changes.addAll(change);
    }
    return changes;
  }

  /**
   * Resolves a nullable return error by generating a code fix. Currently, the only solution we have
   * is if the method is actually returning nullable, we make the method nullable and resolve
   * triggered errors.
   *
   * @param error the error to fix.
   * @return a {@link MethodRewriteChange} that represents the code fix, or {@code NO_ACTION} if the
   *     error cannot be fixed.
   */
  private Set<RegionRewrite> resolveNullableReturnError(NullAwayError error) {
    // Check if it is a false positive
    logger.trace("Checking if the method is actually returning nullable.");
    OnMethod onMethod = new OnMethod(error.path, error.getRegion().clazz, error.getRegion().member);
    Response nullabilityPossibility = gpt.checkNullabilityPossibilityAtErrorPoint(error, context);
    if (nullabilityPossibility.isDisagreement()) {
      logger.trace("False positive detected at return expression.");
      // add SuppressWarnings
      context.injector.injectAnnotation(
          new AddSingleElementAnnotation(
              new OnMethod(error.path, error.getRegion().clazz, error.getRegion().member),
              "SuppressWarnings",
              "NullAway",
              false));
      return NO_ACTION;
    }
    logger.trace("Checking if the method is a getter for field.");
    String returnedField = checkIfMethodIsGetter(onMethod);
    if (returnedField != null) {
      logger.trace("The method is identified as a getter method for field: {}", returnedField);
      OnField onField =
          context
              .targetModuleInfo
              .getFieldRegistry()
              .getLocationOnField(onMethod.clazz, returnedField);
      logger.trace("Checking if the field is nullable.");
      boolean investigateFieldNullability = investigateFieldNullability(onField, returnedField);
      if (!investigateFieldNullability) {
        logger.trace(
            "Field is not nullable. Removed annotation from field and added initializer to the method.");
        return NO_ACTION;
      } else {
        logger.trace("Field is nullable, keeping the annotation on the field.");
      }
    } else {
      logger.trace("Not a getter method.");
    }
    // Make the method nullable.
    context.injector.injectAnnotation(
        new AddMarkerAnnotation(onMethod, context.config.nullableAnnot));
    logger.trace("Made the method nullable and resolving triggered errors.");
    // resolve triggered errors.
    return fixTriggeredErrorsForLocation(onMethod);
  }

  /**
   * Resolves an uninitialized field error by generating a code fix.
   *
   * @param error the error to fix.
   * @return a {@link MethodRewriteChange} that represents the code fix, or {@code null} if the
   */
  private Set<RegionRewrite> resolveUninitializedField(NullAwayError error) {
    // This method is going to analyze the nullability of each field individually.
    Set<RegionRewrite> changes = new HashSet<>();
    String[] names = error.getUninitializedFieldsFromErrorMessage();
    logger.trace("Resolving uninitialized field errors for fields: {}", Arrays.toString(names));
    final int[] index = {0};
    error
        .getResolvingFixes()
        .forEach(
            fix -> {
              if (fix.isOnField()) {
                logger.trace("Working on field: {}", names[index[0]]);
                boolean checkFieldNullability =
                    investigateFieldNullability(fix.toField(), names[index[0]]);
                if (!checkFieldNullability) {
                  logger.trace("Field is not nullable. Removed annotation from field.");
                } else {
                  // Make the field nullable if not already.
                  context.injector.injectAnnotation(
                      new AddMarkerAnnotation(fix.toField(), context.config.nullableAnnot));
                  Set<NullAwayError> triggeredErrors =
                      getTriggeredErrorsFromLocation(fix.toField());
                  if (triggeredErrors.isEmpty()) {
                    logger.trace("Expected to have errors for making the field nullable.");
                    return;
                  }
                  Set<RegionRewrite> c = new HashSet<>();
                  logger.trace("Trying to fix errors for making the field nullable");
                  triggeredErrors.forEach(
                      nullAwayError -> {
                        logger.trace("Working on triggered error: {}", nullAwayError);
                        c.addAll(fix(nullAwayError));
                      });
                  changes.addAll(c);
                }
              }
            });
    return changes;
  }

  /**
   * Resolves a dereference error by generating a code fix.
   *
   * @param error the error to fix.
   * @return a {@link MethodRewriteChange} that represents the code fix, or {@code null} if the
   *     error cannot be fixed.
   */
  private Set<RegionRewrite> resolveDereferenceError(NullAwayError error) {
    logger.trace("Resolving dereference error: {}", error);
    if (ASTParser.isObjectEqualsMethod(error.getRegion().member)) {
      logger.trace("Fixing dereference error in equals method.");
      return gpt.fixDereferenceErrorInEqualsMethod(error);
    }
    if (ASTParser.isObjectToStringMethod(error.getRegion().member)) {
      logger.trace("Fixing dereference error in toString method.");
      return gpt.fixDereferenceErrorInToStringMethod(error);
    }
    if (ASTParser.isObjectHashCodeMethod(error.getRegion().member)) {
      logger.trace("Fixing dereference error in hashCode method.");
      return gpt.fixDereferenceErrorInHashCodeMethod(error);
    }
    // Check nullability possibility.
    logger.trace("Checking nullability possibility at error point");
    Response nullabilityPossibility = gpt.checkNullabilityPossibilityAtErrorPoint(error, context);
    if (nullabilityPossibility.isDisagreement()) {
      logger.trace("False positive detected.");
      // cast to nonnull.
      return constructCastToNonnullChange(error, nullabilityPossibility.getReason());
    }
    if (error.getRegion().isOnCallable()) {
      // check if method already annotated as nullable, return nullable.
      CallableDeclaration<?> enclosingMethodForError =
          parser.getCallableDeclaration(error.getRegion().clazz, error.getRegion().member);
      if (enclosingMethodForError != null
          && parser.isMethodWithNullableReturn(enclosingMethodForError)) {
        // make "return null" statement if null.
        logger.trace(
            "Method is already annotated as nullable. Constructing return null statement.");
        Set<RegionRewrite> changes =
            gpt.fixDereferenceByReturningNullInNullableMethod(error, context);
        apply(changes);
        // add back annotation
        // TODO: REWRITE ONLY METHOD BODY.
        context.injector.injectAnnotation(
            new AddMarkerAnnotation(
                new OnMethod(error.path, error.getRegion().clazz, error.getRegion().member),
                context.config.nullableAnnot));
        return NO_ACTION;
      }
    }
    NullAwayError.NullableExpressionInfo info = error.getNullableExpressionInfo();
    return resolveDereferenceErrorElementType(error, info);
  }

  private Set<RegionRewrite> resolveDereferenceErrorElementType(
      NullAwayError error, NullAwayError.NullableExpressionInfo info) {
    switch (info.kind) {
      case "field":
        OnField onField =
            context.targetModuleInfo.getFieldRegistry().getLocationOnField(info.clazz, info.symbol);
        return resolveFieldNullabilityError(onField, info.symbol);
      case "parameter":
        return resolveParameterDereferenceError(error, info.clazz, info.symbol);
      case "method":
        return resolveMethodDereferenceError(
            error, info.clazz, info.symbol, info.expression, info.isAnnotated);
      case "local_variable":
        JsonArray origins = error.getOrigins();
        Set<RegionRewrite> ans = new HashSet<>();
        for (JsonElement origin : origins) {
          NullAwayError.NullableExpressionInfo i =
              new NullAwayError.NullableExpressionInfo(origin.getAsJsonObject());
          ans.addAll(resolveDereferenceErrorElementType(error, i));
        }
        return ans;
      default:
        return NO_ACTION;
    }
  }

  /**
   * Resolves a method dereference error by generating a code fix.
   *
   * @param error the error to fix.
   * @param encClass the owner of the method.
   * @param method the name of the method.
   * @param invocation invocation expression.
   * @return a {@link MethodRewriteChange} that represents the code fix, or {@code empty} if no fix
   *     is found.
   */
  private Set<RegionRewrite> resolveMethodDereferenceError(
      NullAwayError error, String encClass, String method, String invocation, boolean isAnnotated) {
    // Build a context for prompt generation
    logger.trace("Resolving method dereference error.");
    if (isAnnotated) {
      logger.trace("Method is in annotated package. Checking if the method is returning nullable.");
      boolean isReturningNullable = investigateMethodReturnNullability(encClass, method);
      if (!isReturningNullable) {
        logger.trace("Method is not returning nullable. Injecting suppression annotation.");
        OnMethod methodLocation =
            context
                .targetModuleInfo
                .getMethodRegistry()
                .findMethodByName(encClass, method)
                .location;
        context.injector.removeAnnotation(
            new RemoveMarkerAnnotation(methodLocation, context.config.nullableAnnot));
        context.injector.injectAnnotation(
            new AddSingleElementAnnotation(methodLocation, "SuppressWarnings", "NullAway", false));
        return NO_ACTION;
      }
    }
    Response callSiteNullability =
        isAnnotated
            ? checkIfMethodIsReturningNullableOnCallSite(encClass, method, invocation)
            : checkIfMethodIsReturningNullableOnCallSite(
                error.getRegion().clazz, error.getRegion().member, invocation);
    if (callSiteNullability.isDisagreement()) {
      logger.trace(
          "Method is not returning nullable on call site. Injecting suppression annotation.");
      // Add precondition here.
      return constructCastToNonnullChange(error, callSiteNullability.getReason());
    }
    // Try to fix by regions using the method as an example.
    MethodRecord record =
        context.targetModuleInfo.getMethodRegistry().findMethodByName(encClass, method);
    if (record == null) {
      logger.trace("Method not found: " + encClass + "#" + method);
      logger.trace("Asking simple model to fix dereference error.");

      return basicNullAwayCodeFix.fix(error);
    }
    OnMethod methodLocation = record.location;
    logger.trace("Trying to fix by regions using the method as an example.");
    return fixErrorByRegions(error, methodLocation);
  }

  /**
   * Resolves a parameter dereference error by generating a code fix. At this moment, we only ask if
   * the parameter is actually non-null.
   *
   * @param error the error to fix.
   * @param encClass the owner of the method.
   * @param paramName the name of the parameter.
   * @return a {@link MethodRewriteChange} that represents the code fix, or {@code empty} if no fix
   *     is found.
   */
  private Set<RegionRewrite> resolveParameterDereferenceError(
      NullAwayError error, String encClass, String paramName) {
    logger.trace("Resolving parameter dereference error.");
    // Build a context for prompt generation
    InvocationRecord record =
        invocationRecordRegistry.computeInvocationRecord(encClass, error.getRegion().member, 3);
    int count = 0;
    while (count++ < 10) {
      String callContext = record.constructCallGraphContext();
      Response paramNullabilityPossibility =
          gpt.checkIfParamIsNullable(
              encClass, error.getRegion().member, paramName, callContext, context);
      if (!paramNullabilityPossibility.isSuccessFull()) {
        logger.trace(
            "Could not determine the nullability of the parameter. Model asked for more info.");
        ImmutableSet<String> methods =
            paramNullabilityPossibility.getValuesFromTag("/response/methods", "method");
        if (methods.isEmpty()) {
          throw new IllegalStateException(
              "Could not determine the nullability of the parameter and did not ask for any methods declaration.");
        }
        boolean updated = record.addRequestedMethodsByNames(methods);
        if (!updated) {
          logger.trace("Could not add requested methods by names.");
          break;
        }
      } else {
        if (paramNullabilityPossibility.isDisagreement()) {
          logger.trace("Disagreement in the nullability of the parameter.");
          return NO_ACTION;
        }
        logger.trace("Agreement in the nullability of the parameter.");
        logger.trace("We are not supporting dereference on nullable parameter yet!!!");
        return NO_ACTION;
      }
    }
    return NO_ACTION;
  }

  /**
   * Resolves a field dereference error by generating a code fix.
   *
   * <p>Here are the steps to resolve a field dereference error:
   *
   * <ol>
   *   <li>Look if there is any method initializing this field.
   *   <li>Check if there is any region with safe use of this field.
   *   <li>Consult gpt to generate a fix for each unsafe region.
   *   <li>Try to fix by safe regions if exists.
   *   <li>If no safe region found, of no fix found by safe regions, ask gpt to generate a fix using
   *       default values or rewrite but avoid adding preconditions.
   * </ol>
   *
   * @param onField the field to fix.
   * @param field the field name to fix. A location on field can target multiple inline fields. This
   *     parameter targets a specific field.
   * @return a {@link MethodRewriteChange} that represents the code fix, or {@code empty} if no fix
   *     is found.
   */
  private Set<RegionRewrite> resolveFieldNullabilityError(OnField onField, String field) {
    boolean fieldNullabilityCheck = investigateFieldNullability(onField, field);
    if (!fieldNullabilityCheck) {
      logger.trace("Field is not nullable. removed annotation from field and getter.");
      return NO_ACTION;
    }
    //    InvocationRecord record =
    //        invocationRecordRegistry.computeInvocationRecord(
    //            onField.clazz, ASTParser.getterMethod(field), 3);
    //    String callContext = record.constructCallGraphContext();

    // no initializer found. Try to fix by regions using the method as an example.
    // Make field nullable if not already.
    context.injector.injectAnnotation(
        new AddMarkerAnnotation(onField, context.config.nullableAnnot));
    Set<NullAwayError> triggeredErrors = getTriggeredErrorsFromLocation(onField);
    if (triggeredErrors.isEmpty()) {
      logger.trace("Expected to have errors for making the field nullable.");
      return NO_ACTION;
    }
    Set<RegionRewrite> changes = new HashSet<>();
    logger.trace("Trying to fix errors for making the field nullable");
    triggeredErrors.forEach(e -> changes.addAll(fixErrorByRegions(e, onField)));
    return changes;
  }

  /**
   * Attempts to fix an error by identifying all impacted regions based on the given location and
   * generating fixes using example-based reasoning. Prioritizes safe regions for generating fixes.
   *
   * <p>The method categorizes regions into safe and unsafe based on whether they contain errors. If
   * an unsafe region has an error, it first attempts to generate a fix using safe regions. If that
   * fails, it attempts to generate a fix using all regions.
   *
   * @param error the error to fix.
   * @param location the location of the error.
   * @return a set of {@link MethodRewriteChange} instances representing the code fix, or an empty
   *     set if no fix is found.
   */
  private Set<RegionRewrite> fixErrorByRegions(NullAwayError error, Location location) {
    if (!error.isFixableByRegionExample()) {
      return Set.of();
    }
    logger.trace("Fixing error by regions.");
    Set<Region> impactedRegions =
        context.targetModuleInfo.getRegionRegistry().getImpactedRegions(location);
    Set<NullAwayError> triggeredErrors = getTriggeredErrorsFromLocation(location);
    Set<Region> unsafeRegions =
        triggeredErrors.stream().map(Error::getRegion).collect(Collectors.toSet());
    Set<Region> safeRegions =
        impactedRegions.stream()
            .filter(region -> !unsafeRegions.contains(region))
            .collect(Collectors.toSet());
    Set<RegionRewrite> changes = new HashSet<>();
    logger.trace("Safe regions: {} - Unsafe regions: {}", safeRegions.size(), unsafeRegions.size());
    Set<RegionRewrite> changesForRegion = NO_ACTION;
    if (!safeRegions.isEmpty()) {
      // First try to fix by safe regions if exists.
      changesForRegion = gpt.fixDereferenceErrorBySafeRegions(error, safeRegions, context);
    }
    if (changesForRegion.isEmpty()) {
      logger.trace("No fix found by safe regions. Trying to fix by all regions.");
      // If no safe region found, of no fix found by safe regions, try to fix by precondition
      // check.
      changesForRegion =
          gpt.fixDereferenceErrorByAllRegions(error, safeRegions, unsafeRegions, context);
    }
    if (!changesForRegion.isEmpty()) {
      logger.trace("Successfully generated a fix for the error.");
      changes.addAll(changesForRegion);
    } else {
      logger.trace("-----------Could not generate a fix for error-----------\n{}", error);
    }
    return changes;
  }

  /**
   * Constructs a {@link MethodRewriteChange} that casts the dereferenced nullable value shown in
   * the error to non-null.
   *
   * <pre>{@code
   * void foo(@Nullable Collection<?> coll){
   * boolean isEmpty = coll == null || coll.isEmpty();
   * if(!isEmpty) {
   *  Nullability.castToNonnull(coll).deref();
   * }}
   * }</pre>
   *
   * @param error the error to fix.
   * @return a {@link MethodRewriteChange} that represents the code fix.
   */
  private Set<RegionRewrite> constructCastToNonnullChange(NullAwayError error, String reason) {
    logger.trace("Constructing cast to nonnull change for reason: {}", reason);
    return gpt.fixDereferenceByAddingCastToNonnull(error, reason, context);
  }

  /**
   * Fixes the triggered errors for annotating the given location with {@code @Nullable}. It
   * resolves all the triggered errors by adding annotations and rewriting the code.
   *
   * @param location the location to fix the triggered errors for.
   * @return the set of {@link MethodRewriteChange} instances representing the code fix.
   */
  private Set<RegionRewrite> fixTriggeredErrorsForLocation(Location location) {
    logger.trace("Fixing triggered errors for location: {}", location);
    Set<NullAwayError> errors = getTriggeredErrorsFromLocation(location);
    logger.trace("Triggered errors size: {}", errors.size());
    // add annotations for resolvable errors.
    Set<Fix> fixes =
        errors.stream()
            .filter(e -> !e.getResolvingFixes().isEmpty())
            .map(e -> e.getResolvingFixes().iterator().next())
            .collect(Collectors.toSet());
    logger.trace("Adding annotations for resolvable errors, size: {}", fixes.size());
    context.injector.injectFixes(fixes);
    // resolve the ones where annotation cannot fix
    Set<NullAwayError> unresolvableErrors =
        errors.stream().filter(e -> e.getResolvingFixes().isEmpty()).collect(Collectors.toSet());
    Set<RegionRewrite> changes = new HashSet<>();
    for (NullAwayError unresolvableError : unresolvableErrors) {
      logger.trace("Resolving unresolvable error for triggered error: {}", unresolvableError);
      Set<RegionRewrite> change = fix(unresolvableError);
      changes.addAll(change);
    }
    return changes;
  }

  /**
   * Checks if the method is returning nullable.
   *
   * @param encClass the owner of the method.
   * @param method the name of the method.
   * @return true if the method is returning nullable.
   */
  private boolean investigateMethodReturnNullability(String encClass, String method) {
    logger.trace("Checking if the method is returning nullable.");
    // Build a record, we only need the method declaration so we set the depth to 1.
    InvocationRecord record = invocationRecordRegistry.computeInvocationRecord(encClass, method, 1);
    int count = 0;
    while (count++ < 10) {
      String callContext = record.constructCallGraphContext();
      Response methodNullability =
          gpt.checkIfMethodIsReturningNullable(encClass, method, callContext, context);
      if (!methodNullability.isSuccessFull()) {
        ImmutableSet<String> methods =
            methodNullability.getValuesFromTag("/response/methods", "method");
        if (methods.isEmpty()) {
          logger.trace("Could not determine the nullability and model did not ask for any method.");
          return true;
        }
        boolean updated = record.addRequestedMethodsByNames(methods);
        if (!updated) {
          logger.trace("Could not add requested methods by names.");
          break;
        }
      } else {
        if (methodNullability.isDisagreement()) {
          return false;
        }
        if (methodNullability.isAgreement()) {
          return true;
        }
      }
    }
    // At this moment, just to be safe, we assume it is returning nullable.
    return true;
  }

  /**
   * Investigates the nullability of a field by checking if there is any method initializing it.
   *
   * @param onField the field to investigate.
   * @param field the field name to investigate. A location on field can target multiple inline
   *     fields.
   * @return true if the field is nullable, false otherwise.
   */
  private boolean investigateFieldNullability(OnField onField, String field) {
    logger.trace("Investigating field nullability.");
    logger.trace("Checking if there is any method initializing field: {}", field);
    Set<OnMethod> methods =
        context
            .targetModuleInfo
            .getFieldInitializationStore()
            .findInitializerForField(onField.clazz, field);
    if (!methods.isEmpty()) {
      // TODO: Maybe we should pick the best candidate rather than the first one.
      // continue with the initializer.
      Optional<AddAnnotation> initializerAnnotation =
          methods.stream()
              .filter(candidate -> gpt.checkIfMethodIsAnInitializer(candidate, context))
              .findFirst()
              .map(
                  method ->
                      new AddMarkerAnnotation(
                          new OnMethod(method.path, method.clazz, method.method),
                          context.config.initializerAnnot));
      if (initializerAnnotation.isPresent()) {
        logger.trace("Found initializer method. Injecting initializer annotation.");
        // remove annotation from field
        RemoveMarkerAnnotation removeAnnotation =
            new RemoveMarkerAnnotation(onField, context.config.nullableAnnot);
        // Remove annotation from getter method if exists.
        Optional<MethodRecord> getterMethod =
            context
                .targetModuleInfo
                .getMethodRegistry()
                .getAllMethodsForClass(onField.clazz)
                .stream()
                .filter(
                    record -> {
                      CallableDeclaration<?> callable =
                          parser.getCallableDeclaration(
                              record.location.clazz, record.location.method);
                      if (!(callable instanceof MethodDeclaration)) {
                        return false;
                      }
                      MethodDeclaration methodDeclaration = (MethodDeclaration) callable;
                      return methodDeclaration.getBody().isPresent()
                          && methodDeclaration
                              .getBody()
                              .get()
                              .toString()
                              .replaceAll("\\s", "")
                              .equals(String.format("{return%s;}", field));
                    })
                .findFirst();
        if (getterMethod.isPresent()) {
          logger.trace(
              "Found getter method. Removing nullable annotation: {}",
              getterMethod.get().location.method);
          RemoveMarkerAnnotation removeAnnotationOnGetter =
              new RemoveMarkerAnnotation(
                  new OnMethod(
                      getterMethod.get().location.path,
                      getterMethod.get().location.clazz,
                      getterMethod.get().location.method),
                  context.config.nullableAnnot);
          context.injector.removeAnnotation(removeAnnotationOnGetter);
        }
        // Add initializer annotation to initializer method.
        context.injector.injectAnnotation(initializerAnnotation.get());
        // Remove nullable from field.
        context.injector.removeAnnotation(removeAnnotation);
        return false;
      }
    }
    return true;
  }

  /**
   * Checks if the given method is a getter for one the enclosing class's field and returns the name
   * of the corresponding field.
   *
   * @param onMethod the method to check.
   * @return field name if the method is a getter, null otherwise.
   */
  @Nullable
  private String checkIfMethodIsGetter(OnMethod onMethod) {
    ImmutableSet<OnField> fields =
        context.targetModuleInfo.getFieldRegistry().getDeclaredFieldsInClass(onMethod.clazz);
    Set<String> names =
        fields.stream().flatMap(field -> field.variables.stream()).collect(Collectors.toSet());
    String methodName = onMethod.method.toLowerCase(Locale.getDefault());
    for (String name : names) {
      String lowerCaseName = name.toLowerCase(Locale.getDefault());
      boolean nameMatchesGetter =
          methodName.contains("get" + lowerCaseName) || methodName.contains("is" + lowerCaseName);
      if (!nameMatchesGetter) {
        continue;
      }
      SourceCode sourceCode =
          parser.getRegionSourceCode(new Region(onMethod.clazz, onMethod.method));
      if (sourceCode == null) {
        continue;
      }
      // check content:
      String content = sourceCode.content.replaceAll("\\s+", "");
      content = content.toLowerCase(Locale.getDefault());
      boolean contentMatchesGetter =
          content.contains(methodName + "{return" + lowerCaseName + ";}")
              || content.contains(methodName + "{return" + "this." + lowerCaseName + ";}");
      if (contentMatchesGetter) {
        return name;
      }
    }
    return null;
  }

  /**
   * Checks if the method is returning nullable on call site.
   *
   * @param encClass the owner of the method.
   * @param method the method signature.
   * @param invocation the invocation expression.
   * @return response indicating the nullability of the method on call site.
   */
  private Response checkIfMethodIsReturningNullableOnCallSite(
      String encClass, String method, String invocation) {
    // Build a record, we only need the method declaration so we set the depth to 1.
    InvocationRecord record = invocationRecordRegistry.computeInvocationRecord(encClass, method, 3);
    int count = 0;
    while (count++ < 10) {
      Response invocationNullability =
          gpt.checkIfMethodIsReturningNullableOnCallSite(invocation, record, context);
      if (!invocationNullability.isSuccessFull()) {
        ImmutableSet<String> methods =
            invocationNullability.getValuesFromTag("/response/methods", "method");
        if (methods.isEmpty()) {
          throw new IllegalStateException(
              "Could not determine the nullability of the invocation and did not ask for any methods declaration.");
        }
        boolean updated = record.addRequestedMethodsByNames(methods);
        if (!updated) {
          logger.trace("Could not add requested methods by names.");
          break;
        }
      } else {
        return invocationNullability;
      }
    }
    // At this moment, just to be safe, we assume it is returning nullable.
    return Response.agree();
  }

  /**
   * Fetches the report from the cache that contains at least one of the resolving fixes.
   *
   * @param error the error to fetch the report for.
   * @return the report that contains at least one of the resolving fixes.
   */
  private ImmutableSet<Error> getTriggeredErrorsFromError(NullAwayError error) {
    // TODO: This is a temporary solution. We need to find a better way to fetch the report.
    Report report =
        reports.stream()
            .filter(r -> error.getResolvingFixes().contains(r.root))
            .findFirst()
            .orElse(null);
    if (report != null) {
      return report.triggeredErrors;
    }
    logger.trace("Impact not found, re-evaluating the location.");
    ImmutableSet<Fix> fixes = error.getResolvingFixes();
    context.config.depth = 1;
    // Initializing required evaluator instances.
    TargetModuleSupplier supplier =
        new TargetModuleSupplier(context, new TargetModuleCache(), new VoidDownstreamImpactCache());
    Evaluator evaluator = new BasicEvaluator(supplier);
    // Result of the iteration analysis.
    ImmutableSet<Report> newReports = evaluator.evaluate(fixes);
    return newReports.stream()
        .flatMap(input -> input.triggeredErrors.stream())
        .collect(ImmutableSet.toImmutableSet());
  }

  /**
   * Fetches the report from the cache that contains the given location.
   *
   * @param location the location to fetch the report for.
   * @return the report that contains the given location.
   */
  private Set<NullAwayError> getTriggeredErrorsFromLocation(Location location) {
    Report report =
        reports.stream()
            .filter(
                r -> r.root.toLocations().size() == 1 && r.root.toLocations().contains(location))
            .findFirst()
            .orElse(null);
    if (report != null) {
      return report.triggeredErrors.stream()
          .map(e -> (NullAwayError) e)
          .collect(Collectors.toSet());
    }
    logger.trace("Impact not found, re-evaluating the location.");
    RemoveAnnotation removeAnnotation =
        new RemoveMarkerAnnotation(location, context.config.nullableAnnot);
    context.injector.removeAnnotation(removeAnnotation);
    ImmutableSet<Fix> fixes =
        ImmutableSet.of(new Fix(new AddMarkerAnnotation(location, context.config.nullableAnnot)));
    context.config.depth = 1;
    // Initializing required evaluator instances.
    TargetModuleSupplier supplier =
        new TargetModuleSupplier(context, new TargetModuleCache(), new VoidDownstreamImpactCache());
    Evaluator evaluator = new BasicEvaluator(supplier);
    // Result of the iteration analysis.
    Utility.buildTarget(context);
    ImmutableSet<Report> newReports = evaluator.evaluate(fixes);
    // Add back the annotation.
    context.injector.injectAnnotation(
        new AddMarkerAnnotation(location, context.config.nullableAnnot));
    return newReports.stream()
        .flatMap(input -> input.triggeredErrors.stream())
        .map(e -> (NullAwayError) e)
        .collect(Collectors.toSet());
  }
}
