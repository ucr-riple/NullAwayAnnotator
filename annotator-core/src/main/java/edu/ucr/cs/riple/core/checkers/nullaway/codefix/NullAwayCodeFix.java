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

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.Context;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.cache.TargetModuleCache;
import edu.ucr.cs.riple.core.cache.downstream.VoidDownstreamImpactCache;
import edu.ucr.cs.riple.core.checkers.nullaway.NullAway;
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
import edu.ucr.cs.riple.injector.Injector;
import edu.ucr.cs.riple.injector.SourceCode;
import edu.ucr.cs.riple.injector.changes.AddAnnotation;
import edu.ucr.cs.riple.injector.changes.AddMarkerAnnotation;
import edu.ucr.cs.riple.injector.changes.AddSingleElementAnnotation;
import edu.ucr.cs.riple.injector.changes.MethodRewriteChange;
import edu.ucr.cs.riple.injector.changes.RemoveAnnotation;
import edu.ucr.cs.riple.injector.changes.RemoveMarkerAnnotation;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.injector.location.OnMethod;
import edu.ucr.cs.riple.injector.util.ASTUtils;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** A class that provides code fixes for {@link NullAwayError}s. */
public class NullAwayCodeFix {

  /** The {@link ChatGPT} instance used to generate code fixes. */
  private final ChatGPT gpt;

  /** The {@link Injector} instance used to apply code fixes. */
  private final Injector injector;

  /** Annotation context instance. */
  private final Context context;

  /** Parser used to parse the source code of the file containing the error. */
  private final ASTParser parser;

  private ImmutableSet<Report> reports;

  /** Invocation record registry to retrieve the callers of a method. */
  private final InvocationRecordRegistry invocationRecordRegistry;

  /** The logger instance. */
  private static final Logger logger = LogManager.getLogger(NullAwayCodeFix.class);

  /**
   * Simply returns an empty set meaning no action is needed. The purpose is only increasing
   * readability.
   */
  private static final Set<MethodRewriteChange> NO_ACTION = Set.of();

  public NullAwayCodeFix(Context context) {
    this.parser = new ASTParser(context);
    this.gpt = new ChatGPT(parser);
    this.context = context;
    this.injector = new Injector(context.config.languageLevel);
    this.invocationRecordRegistry = new InvocationRecordRegistry(context.targetModuleInfo);
  }

  /**
   * Collects the impacts of adding rejected fixes on source code along with their triggered errors
   * only to depth 1.
   */
  public void collectImpacts() {
    // Suggested fixes of target at the current state.
    ImmutableSet<Fix> fixes =
        Utility.readFixesFromOutputDirectory(context, context.targetModuleInfo).stream()
            .collect(ImmutableSet.toImmutableSet());
    // Initializing required evaluator instances.
    // For now lets only focus on depth 1.
    context.config.depth = 1;
    TargetModuleSupplier supplier =
        new TargetModuleSupplier(context, new TargetModuleCache(), new VoidDownstreamImpactCache());
    Evaluator evaluator = new BasicEvaluator(supplier);
    // Result of the iteration analysis.
    this.reports = evaluator.evaluate(fixes);
  }

  /**
   * Generates a code fix for the given {@link NullAwayError}. The fix is rewrites of sets of
   * methods.
   *
   * @param error the error to fix.
   * @return a {@link MethodRewriteChange} that represents the code fix, or {@code null} if the
   *     error cannot be fixed.
   */
  public Set<MethodRewriteChange> fix(NullAwayError error) {
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
        return NO_ACTION;
    }
  }

  /**
   * Applies the given {@link MethodRewriteChange} to the source code.
   *
   * @param changes the changes to apply.
   */
  public void apply(Set<MethodRewriteChange> changes) {
    changes.forEach(injector::rewriteMethod);
  }

  /**
   * Resolves a wrong override return error by generating a code fix. Currently, the only solution
   * we make the super method nullable and resolve triggered errors.
   *
   * @param error the error to fix.
   * @return a {@link MethodRewriteChange} that represents the code fix, or {@code NO_ACTION} if the
   *     error cannot be fixed.
   */
  private Set<MethodRewriteChange> resolveWrongOverrideReturnError(NullAwayError error) {
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
  private Set<MethodRewriteChange> resolveAssignFieldNullableError(NullAwayError error) {
    logger.trace("Resolving assign field nullable error.");
    // currently the only solution we follow is to make the field nullable and resolve triggered
    // errors.
    Preconditions.checkArgument(error.getResolvingFixes().size() == 1);
    // Make the field nullable.
    Fix fix = error.getResolvingFixes().iterator().next();
    logger.trace("Making the field nullable.");
    context.injector.injectFixes(Set.of(fix));
    Report report = fetchReport(error);
    if (report == null) {
      return NO_ACTION;
    }
    // Add any annotations that triggered errors contain:
    logger.trace("Adding all triggered annotations.");
    Set<Fix> fixes =
        report.triggeredErrors.stream()
            .map(Error::getResolvingFixes)
            .flatMap(Set::stream)
            .collect(ImmutableSet.toImmutableSet());
    context.injector.injectFixes(fixes);
    // Resolve the ones where annotation cannot fix
    Set<NullAwayError> unresolvableErrors =
        report.triggeredErrors.stream()
            .filter(e -> e.getResolvingFixes().isEmpty())
            .map(e -> (NullAwayError) e)
            .collect(Collectors.toSet());
    Set<MethodRewriteChange> changes = new HashSet<>();
    logger.trace("Resolving unresolvable errors.");
    for (NullAwayError unresolvableError : unresolvableErrors) {
      logger.trace("Resolving unresolvable error: {}", unresolvableError);
      Set<MethodRewriteChange> change = fix(unresolvableError);
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
  private Set<MethodRewriteChange> resolveNullableReturnError(NullAwayError error) {
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
    // Make the method nullable.
    context.injector.injectAnnotation(
        new AddMarkerAnnotation(onMethod, context.config.nullableAnnot));
    // resolve triggered errors.
    return fixTriggeredErrorsForLocation(onMethod);
  }

  /**
   * Resolves an uninitialized field error by generating a code fix.
   *
   * @param error the error to fix.
   * @return a {@link MethodRewriteChange} that represents the code fix, or {@code null} if the
   */
  private Set<MethodRewriteChange> resolveUninitializedField(NullAwayError error) {
    // This method is going to analyze the nullability of each field individually.
    Set<MethodRewriteChange> changes = new HashSet<>();
    String[] names = NullAwayError.extractPlaceHolderValue(error);
    logger.trace("Resolving uninitialized field errors for fields: {}", Arrays.toString(names));
    final int[] index = {0};
    error
        .getResolvingFixes()
        .forEach(
            fix -> {
              if (fix.isOnField()) {
                logger.trace("Working on field: {}", names[index[0]]);
                changes.addAll(
                    resolveFieldDereferenceError(error, fix.toField(), names[index[0]++]));
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
  private Set<MethodRewriteChange> resolveDereferenceError(NullAwayError error) {
    if (ASTParser.isObjectEqualsMethod(error.getRegion().member)) {
      logger.trace("Fixing dereference error in equals method.");
      return gpt.fixDereferenceErrorInEqualsMethod(error, context);
    }
    if (ASTParser.isObjectToStringMethod(error.getRegion().member)) {
      logger.trace("Fixing dereference error in toString method.");
      return gpt.fixDereferenceErrorInToStringMethod(error, context);
    }
    if (ASTParser.isObjectHashCodeMethod(error.getRegion().member)) {
      logger.trace("Fixing dereference error in hashCode method.");
      return gpt.fixDereferenceErrorInHashCodeMethod(error, context);
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
        // make return null statement if null.
        logger.trace(
            "Method is already annotated as nullable. Constructing return null statement.");
        return constructReturnNullIfExpressionIsNullForError(error);
      }
    }
    String[] infos = NullAwayError.extractPlaceHolderValue(error);
    String expression = infos[0];
    String type = infos[1];
    String encClass = infos[2];
    String expressionSymbol = infos[4];
    boolean isAnnotated = infos[3].equalsIgnoreCase("true");
    switch (type) {
      case "field":
        OnField onField =
            context.targetModuleInfo.getFieldRegistry().getLocationOnField(encClass, expression);
        return resolveFieldDereferenceError(error, onField, expression);
      case "parameter":
        return resolveParameterDereferenceError(error, encClass, expression);
      case "method":
        return resolveMethodDereferenceError(
            error, encClass, expressionSymbol, expression, isAnnotated);
      case "local_variable":
        logger.trace("not supporting dereference on local variable yet.");
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
  private Set<MethodRewriteChange> resolveMethodDereferenceError(
      NullAwayError error, String encClass, String method, String invocation, boolean isAnnotated) {
    // Build a context for prompt generation
    logger.trace("Resolving method dereference error.");
    if (isAnnotated) {
      logger.trace("Method is in annotated package. Checking if the method is returning nullable.");
      boolean isReturningNullable = checkIfMethodIsReturningNullable(encClass, method);
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
      Response callSiteNullability =
          checkIfMethodIsReturningNullableOnCallSite(encClass, method, invocation);
      if (callSiteNullability.isDisagreement()) {
        logger.trace(
            "Method is not returning nullable on call site. Injecting suppression annotation.");
        // Add precondition here.
        return constructCastToNonnullChange(error, callSiteNullability.getReason());
      }
    }
    // Try to fix by regions using the method as an example.
    MethodRecord record =
        context.targetModuleInfo.getMethodRegistry().findMethodByName(encClass, method);
    if (record == null) {
      logger.trace("Method not found: " + encClass + "#" + method);
      return NO_ACTION;
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
  private Set<MethodRewriteChange> resolveParameterDereferenceError(
      NullAwayError error, String encClass, String paramName) {
    logger.trace("Resolving parameter dereference error.");
    // Build a context for prompt generation
    InvocationRecord record =
        invocationRecordRegistry.computeInvocationRecord(encClass, error.getRegion().member, 3);
    int count = 0;
    while (count++ < 10) {
      String callContext = record.constructCallGraphContext(parser);
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
        record.addRequestedMethodsByNames(methods);
      } else {
        if (paramNullabilityPossibility.isDisagreement()) {
          logger.trace("Disagreement in the nullability of the parameter.");
          return NO_ACTION;
        }
        logger.trace("Agreement in the nullability of the parameter.");
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
   *   <li>Check if there is an assignment to the expression in the method body.
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
  private Set<MethodRewriteChange> resolveFieldDereferenceError(
      NullAwayError error, OnField onField, String field) {
    logger.trace("Investigating field nullability.");
    logger.trace("Checking if there is any method initializing this field.");
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
        // Add annotation
        context.injector.injectAnnotation(initializerAnnotation.get());
        // remove nullable
        context.injector.removeAnnotation(removeAnnotation);
        return NO_ACTION;
      }
    }
    // no initializer found. Try to fix by regions using the method as an example.
    // Make field nullable if not already.
    context.injector.injectAnnotation(
        new AddMarkerAnnotation(onField, context.config.nullableAnnot));
    return fixErrorByRegions(error, onField);
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
  private Set<MethodRewriteChange> fixErrorByRegions(NullAwayError error, Location location) {
    logger.trace("Fixing error by regions.");
    Set<Region> impactedRegions =
        context.targetModuleInfo.getRegionRegistry().getImpactedRegions(location);
    Set<NullAwayError> triggeredErrors = getTriggeredErrorsForLocation(location);
    Set<Region> unsafeRegions =
        triggeredErrors.stream().map(Error::getRegion).collect(Collectors.toSet());
    Set<Region> safeRegions = new HashSet<>();

    impactedRegions.forEach(
        region -> {
          if (!unsafeRegions.contains(region)) {
            safeRegions.add(region);
          }
        });
    Set<MethodRewriteChange> changes = new HashSet<>();
    logger.trace("Safe regions: {} - Unsafe regions: {}", safeRegions.size(), unsafeRegions.size());
    Set<MethodRewriteChange> changesForRegion = NO_ACTION;
    if (error.messageType.equals("DEREFERENCE_NULLABLE")) {
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
        logger.trace("Could not generate a fix for error: {}", error);
      }
    } else {
      changes.addAll(fix(error));
    }
    return changes;
  }

  /**
   * Constructs a {@link MethodRewriteChange} that returns null right before if the expression shown
   * in the error is null.
   *
   * <p>Example:
   *
   * <pre>{@code
   * @Nullable Object foo() {
   *  +if (exp == null) {
   *  +    return null;
   *  +}
   *  return exp.deref();
   * }
   * }</pre>
   *
   * @param error the error to fix.
   * @return a {@link MethodRewriteChange} that represents the code fix.
   */
  private Set<MethodRewriteChange> constructReturnNullIfExpressionIsNullForError(
      NullAwayError error) {
    SourceCode enclosingMethod = parser.getRegionSourceCode(error.getRegion());
    if (enclosingMethod == null) {
      return NO_ACTION;
    }
    String[] lines = enclosingMethod.content.split("\n");
    // calculate the erroneous line in method. We have to adjust the line number to the method's
    // range. Note that the line number is 1-based in java parser, and we need to adjust it to
    // 0-based.
    int errorLine = error.position.lineNumber - (enclosingMethod.range.begin.line - 1);
    String expression = NullAwayError.extractPlaceHolderValue(error)[0];
    String whitespace = Utility.getLeadingWhitespace(lines[errorLine]);
    String returnNullStatement =
        String.format(
            "%sif (%s == null) {\n%s\treturn null;\n%s}\n",
            whitespace, expression, whitespace, whitespace);
    lines[errorLine] = returnNullStatement + lines[errorLine];
    MethodRewriteChange change =
        new MethodRewriteChange(
            new OnMethod(error.path, error.getRegion().clazz, error.getRegion().member),
            String.join("\n", lines));
    return Set.of(change);
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
  private Set<MethodRewriteChange> constructCastToNonnullChange(
      NullAwayError error, String reason) {
    logger.trace("Constructing cast to nonnull change.");
    SourceCode enclosingMethod = parser.getRegionSourceCode(error.getRegion());
    if (enclosingMethod == null) {
      return NO_ACTION;
    }
    String[] lines = enclosingMethod.content.split("\n");
    // calculate the erroneous line in method. We have to adjust the line number to the method's
    // range. Note that the line number is 1-based in java parser, and we need to adjust it to
    // 0-based.
    int errorLine = error.position.lineNumber - (enclosingMethod.range.begin.line - 1);
    String expression = NullAwayError.extractPlaceHolderValue(error)[0];
    String castToNonnullStatement =
        String.format("Nullability.castToNonnull(%s, \"%s\")", expression, "reason...");
    int start = lines[errorLine].indexOf(expression);
    int end = start + expression.length();
    lines[errorLine] =
        lines[errorLine].substring(0, start)
            + castToNonnullStatement
            + lines[errorLine].substring(end);
    MethodRewriteChange change =
        new MethodRewriteChange(
            new OnMethod(error.path, error.getRegion().clazz, error.getRegion().member),
            String.join("\n", lines),
            // Add the import required for Preconditions.
            Set.of(NullAway.CAST_TO_NONNULL));
    return Set.of(change);
  }

  /**
   * Returns the set of triggered errors if the given location is annotated with {@code @Nullable}.
   *
   * @param location the location to get the triggered errors for.
   * @return the set of triggered errors for the given location.
   */
  private Set<NullAwayError> getTriggeredErrorsForLocation(Location location) {
    Report report = fetchReport(location);
    if (report == null) {
      Utility.buildTarget(context);
      Set<NullAwayError> currentErrors =
          Utility.readErrorsFromOutputDirectory(
              context, context.targetModuleInfo, NullAwayError.class);
      // TODO fix this, this is very time consuming.
      RemoveAnnotation removeAnnotation =
          new RemoveMarkerAnnotation(location, context.config.nullableAnnot);
      context.injector.removeAnnotation(removeAnnotation);
      Utility.buildTarget(context);
      Set<NullAwayError> newErrors =
          Utility.readErrorsFromOutputDirectory(
              context, context.targetModuleInfo, NullAwayError.class);
      // Add back the annotation.
      context.injector.injectAnnotation(
          new AddMarkerAnnotation(location, context.config.nullableAnnot));
      // currentErrors - newErrors
      Set<NullAwayError> triggeredErrors = new HashSet<>(currentErrors);
      triggeredErrors.removeAll(newErrors);
      return triggeredErrors;
    }
    return report.triggeredErrors.stream().map(e -> (NullAwayError) e).collect(Collectors.toSet());
  }

  /**
   * Fixes the triggered errors for annotating the given location with {@code @Nullable}. It
   * resolves all the triggered errors by adding annotations and rewriting the code.
   *
   * @param location the location to fix the triggered errors for.
   * @return the set of {@link MethodRewriteChange} instances representing the code fix.
   */
  private Set<MethodRewriteChange> fixTriggeredErrorsForLocation(Location location) {
    logger.trace("Fixing triggered errors for location: {}", location);
    Set<NullAwayError> errors = getTriggeredErrorsForLocation(location);
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
    Set<MethodRewriteChange> changes = new HashSet<>();
    for (NullAwayError unresolvableError : unresolvableErrors) {
      logger.trace("Resolving unresolvable error for triggered error: {}", unresolvableError);
      Set<MethodRewriteChange> change = fix(unresolvableError);
      changes.addAll(change);
    }
    return changes;
  }

  /**
   * Checks if the expression is initialized before use in the method body. TODO: this method only
   * looks for all assignments flow insensitive. We need to make it flow sensitive.
   *
   * @param declaration the method declaration.
   * @param field the field to check for initialization.
   * @return true if the expression is initialized before use in the method body.
   */
  private boolean checkForInitializationBeforeUse(
      CallableDeclaration<?> declaration, String field) {
    // check if the expression is assigned in the method body.
    Iterator<Node> treeIterator = new ASTUtils.DirectMethodParentIterator(declaration);
    while (treeIterator.hasNext()) {
      Node n = treeIterator.next();
      if (n instanceof VariableDeclarationExpr) {
        VariableDeclarationExpr v = (VariableDeclarationExpr) n;
        if (v.getVariables().stream()
            .anyMatch(variableDeclarator -> variableDeclarator.getNameAsString().equals(field))) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Checks if the method is returning nullable.
   *
   * @param encClass the owner of the method.
   * @param method the name of the method.
   * @return true if the method is returning nullable.
   */
  private boolean checkIfMethodIsReturningNullable(String encClass, String method) {
    logger.trace("Checking if the method is returning nullable.");
    // Build a record, we only need the method declaration so we set the depth to 1.
    InvocationRecord record = invocationRecordRegistry.computeInvocationRecord(encClass, method, 1);
    int count = 0;
    while (count++ < 10) {
      String callContext = record.constructCallGraphContext(parser);
      Response methodNullability =
          gpt.checkIfMethodIsReturningNullable(encClass, method, callContext, context);
      if (!methodNullability.isSuccessFull()) {
        ImmutableSet<String> methods =
            methodNullability.getValuesFromTag("/response/methods", "method");
        if (methods.isEmpty()) {
          throw new IllegalStateException(
              "Could not determine the nullability of the method return and did not ask for any methods declaration.");
        }
        record.addRequestedMethodsByNames(methods);
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
      String callContext = record.constructCallGraphContext(parser);
      Response invocationNullability =
          gpt.checkIfMethodIsReturningNullableOnCallSite(invocation, callContext, context);
      if (!invocationNullability.isSuccessFull()) {
        ImmutableSet<String> methods =
            invocationNullability.getValuesFromTag("/response/methods", "method");
        if (methods.isEmpty()) {
          throw new IllegalStateException(
              "Could not determine the nullability of the invocation and did not ask for any methods declaration.");
        }
        record.addRequestedMethodsByNames(methods);
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
  @Nullable
  private Report fetchReport(NullAwayError error) {
    // TODO: This is a temporary solution. We need to find a better way to fetch the report.
    return reports.stream()
        .filter(report -> error.getResolvingFixes().contains(report.root))
        .findFirst()
        .orElse(null);
  }

  /**
   * Fetches the report from the cache that contains the given location.
   *
   * @param location the location to fetch the report for.
   * @return the report that contains the given location.
   */
  @Nullable
  private Report fetchReport(Location location) {
    return reports.stream()
        .filter(
            report ->
                report.root.toLocations().size() == 1
                    && report.root.toLocations().contains(location))
        .findFirst()
        .orElse(null);
  }
}
