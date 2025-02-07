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
import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.Context;
import edu.ucr.cs.riple.core.checkers.nullaway.NullAway;
import edu.ucr.cs.riple.core.checkers.nullaway.NullAwayError;
import edu.ucr.cs.riple.core.checkers.nullaway.codefix.agent.ChatGPT;
import edu.ucr.cs.riple.core.checkers.nullaway.codefix.agent.Response;
import edu.ucr.cs.riple.core.registries.index.Error;
import edu.ucr.cs.riple.core.registries.index.ErrorStore;
import edu.ucr.cs.riple.core.registries.invocation.InvocationRecord;
import edu.ucr.cs.riple.core.registries.invocation.InvocationRecordRegistry;
import edu.ucr.cs.riple.core.registries.method.MethodRecord;
import edu.ucr.cs.riple.core.registries.region.Region;
import edu.ucr.cs.riple.core.util.ASTParser;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.Injector;
import edu.ucr.cs.riple.injector.SourceCode;
import edu.ucr.cs.riple.injector.changes.AddAnnotation;
import edu.ucr.cs.riple.injector.changes.AddMarkerAnnotation;
import edu.ucr.cs.riple.injector.changes.MethodRewriteChange;
import edu.ucr.cs.riple.injector.changes.RemoveAnnotation;
import edu.ucr.cs.riple.injector.changes.RemoveMarkerAnnotation;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.injector.location.OnMethod;
import edu.ucr.cs.riple.injector.util.ASTUtils;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

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

  /** Error store to retrieve errors in a region. */
  private final ErrorStore errorStore;

  /** Invocation record registry to retrieve the callers of a method. */
  private final InvocationRecordRegistry invocationRecordRegistry;

  /**
   * Simply returns an empty set meaning no action is needed. The purpose is only increasing
   * readability.
   */
  private static final Set<MethodRewriteChange> NO_ACTION = Set.of();

  public NullAwayCodeFix(Context context) {
    this.parser = new ASTParser(context);
    this.gpt = new ChatGPT(context, parser);
    this.context = context;
    this.injector = new Injector(context.config.languageLevel);
    this.errorStore = new ErrorStore(context, context.targetModuleInfo);
    this.invocationRecordRegistry = new InvocationRecordRegistry(context.targetModuleInfo);
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
    switch (error.messageType) {
      case "DEREFERENCE_NULLABLE":
        return resolveDereferenceError(error);
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
   * Resolves a dereference error by generating a code fix.
   *
   * @param error the error to fix.
   * @return a {@link MethodRewriteChange} that represents the code fix, or {@code null} if the
   *     error cannot be fixed.
   */
  private Set<MethodRewriteChange> resolveDereferenceError(NullAwayError error) {
    if (ASTParser.isObjectEqualsMethod(error.getRegion().member)) {
      return gpt.fixDereferenceErrorInEqualsMethod(error);
    }
    if (ASTParser.isObjectToStringMethod(error.getRegion().member)) {
      return gpt.fixDereferenceErrorInToStringMethod(error);
    }
    if (ASTParser.isObjectHashCodeMethod(error.getRegion().member)) {
      return gpt.fixDereferenceErrorInHashCodeMethod(error);
    }
    // Check if it is a false positive
    if (gpt.checkIfFalsePositiveAtErrorPoint(error)) {
      // cast to nonnull.
      return constructPreconditionCheckMethodRewriteForError(error);
    }
    if (error.getRegion().isOnCallable()) {
      // check if method already annotated as nullable, return nullable.
      CallableDeclaration<?> enclosingMethodForError =
          parser.getCallableDeclaration(error.getRegion().clazz, error.getRegion().member);
      if (enclosingMethodForError != null
          && parser.isMethodWithNullableReturn(enclosingMethodForError)) {
        // make return null statement if null.
        return constructReturnNullIfExpressionIsNullForError(error);
      }
    }
    String[] infos = NullAwayError.extractPlaceHolderValue(error);
    String expression = infos[0];
    String type = infos[1];
    String encClass = infos[2];
    boolean isAnnotated = infos[3].equalsIgnoreCase("true");
    switch (type) {
      case "field":
        return resolveFieldDereferenceError(error, encClass, expression);
      case "parameter":
        return resolveParameterDereferenceError(error, encClass, expression);
      case "method":
        return resolveMethodDereferenceError(error, encClass, expression, isAnnotated);
      default:
        return NO_ACTION;
    }
  }

  /**
   * Resolves a method dereference error by generating a code fix.
   *
   * @param error the error to fix.
   * @param encClass the class containing the method.
   * @param method the method to fix.
   * @param isAnnotated true if the method is annotated as nullable.
   * @return a {@link MethodRewriteChange} that represents the code fix, or {@code empty} if no fix
   */
  private Set<MethodRewriteChange> resolveMethodDereferenceError(
      NullAwayError error, String encClass, String method, boolean isAnnotated) {
    // Build a context for prompt generation
    if (isAnnotated) {
      boolean isReturningNullable = checkIfMethodIsReturningNullable(encClass, method);
      if (!isReturningNullable) {
        OnMethod methodLocation =
            context
                .targetModuleInfo
                .getMethodRegistry()
                .findMethodByName(encClass, method)
                .location;
        RemoveAnnotation removeNullable =
            new RemoveMarkerAnnotation(methodLocation, context.config.nullableAnnot);
        context.injector.removeAnnotations(Set.of(removeNullable));
        return NO_ACTION;
      }
    }
    // Try to fix by regions using the method as an example.
    OnMethod methodLocation =
        context
            .targetModuleInfo
            .getMethodRegistry()
            .findMethodByName(encClass, error.getRegion().member)
            .location;
    return fixErrorByRegions(methodLocation);
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
    // Build a context for prompt generation
    InvocationRecord record =
        invocationRecordRegistry.computeInvocationRecord(encClass, error.getRegion().member, 3);
    int count = 0;
    while (count++ < 10) {
      String callContext = record.constructCallGraphContext(parser);
      Response paramNullabilityPossibility =
          gpt.checkIfParamIsNullable(encClass, error.getRegion().member, paramName, callContext);
      if (!paramNullabilityPossibility.isSuccessFull()) {
        ImmutableSet<String> methods =
            paramNullabilityPossibility.getValuesFromTag("/response/methods", "method");
        if (methods.isEmpty()) {
          throw new IllegalStateException(
              "Could not determine the nullability of the parameter and did not ask for any methods declaration.");
        }
        record.addRequestedMethodsByNames(methods);
      } else {
        if (paramNullabilityPossibility.isDisagreement()) {
          return NO_ACTION;
        }
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
   * @param error the error to fix.
   * @param encClass the class containing the field.
   * @param field the field to fix.
   * @return a {@link MethodRewriteChange} that represents the code fix, or {@code empty} if no fix
   *     is found.
   */
  private Set<MethodRewriteChange> resolveFieldDereferenceError(
      NullAwayError error, String encClass, String field) {
    // check if there is an assignment to the expression in the method body.
    if (error.getRegion().isOnCallable()) {
      CallableDeclaration<?> enclosingMethodForError =
          parser.getCallableDeclaration(error.getRegion().clazz, error.getRegion().member);
      boolean initializedBeforeUse =
          checkForInitializationBeforeUse(enclosingMethodForError, field);
      if (!initializedBeforeUse) {
        // look if there is any method initializing this field.
        Set<OnMethod> methods =
            context
                .targetModuleInfo
                .getFieldInitializationStore()
                .findInitializerForField(encClass, field);
        if (!methods.isEmpty()) {
          // TODO: Maybe we should pick the best candidate rather than the first one.
          // continue with the initializer.
          Optional<AddAnnotation> initializerAnnotation =
              methods.stream()
                  .filter(gpt::checkIfMethodIsAnInitializer)
                  .findFirst()
                  .map(
                      method ->
                          new AddMarkerAnnotation(
                              new OnMethod(method.path, method.clazz, method.method),
                              context.config.initializerAnnot));
          if (initializerAnnotation.isPresent()) {
            // remove annotation from field
            RemoveMarkerAnnotation removeAnnotation =
                new RemoveMarkerAnnotation(
                    context.targetModuleInfo.getFieldRegistry().getLocationOnField(encClass, field),
                    context.config.nullableAnnot);
            // Remove annotation from getter method if exists.
            Optional<MethodRecord> getterMethod =
                context
                    .targetModuleInfo
                    .getMethodRegistry()
                    .getAllMethodsForClass(encClass)
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
              RemoveMarkerAnnotation removeAnnotationOnGetter =
                  new RemoveMarkerAnnotation(
                      new OnMethod(
                          getterMethod.get().location.path,
                          getterMethod.get().location.clazz,
                          getterMethod.get().location.method),
                      context.config.nullableAnnot);
              context.injector.removeAnnotations(Set.of(removeAnnotationOnGetter));
            }
            // Add annotation
            context.injector.injectAnnotations(Set.of(initializerAnnotation.get()));
            // remove nullable
            context.injector.removeAnnotations(Set.of(removeAnnotation));
            return NO_ACTION;
          }
        }
      }
    }
    // no initializer found. Try to fix by regions using the method as an example.
    OnField onField =
        context.targetModuleInfo.getFieldRegistry().getLocationOnField(encClass, field);
    return fixErrorByRegions(onField);
  }

  /**
   * Attempts to fix an error by identifying all impacted regions based on the given location and
   * generating fixes using example-based reasoning. Prioritizes safe regions for generating fixes.
   *
   * <p>The method categorizes regions into safe and unsafe based on whether they contain errors. If
   * an unsafe region has an error, it first attempts to generate a fix using safe regions. If that
   * fails, it attempts to generate a fix using all regions.
   *
   * @param location the location of the error.
   * @return a set of {@link MethodRewriteChange} instances representing the code fix, or an empty
   *     set if no fix is found.
   */
  private Set<MethodRewriteChange> fixErrorByRegions(Location location) {
    Set<Region> unsafeRegions = new HashSet<>();
    Set<Region> safeRegions = new HashSet<>();
    context
        .targetModuleInfo
        .getRegionRegistry()
        .getImpactedRegions(location)
        .forEach(
            region -> {
              if (errorStore.getErrorsInRegion(region).isEmpty()) {
                safeRegions.add(region);
              } else {
                unsafeRegions.add(region);
              }
            });
    Set<MethodRewriteChange> changes = new HashSet<>();
    // for each unsafe region, consult gpt to generate a fix.
    for (Region region : unsafeRegions) {
      Optional<Error> optionalError = errorStore.getErrorsInRegion(region).stream().findAny();
      if (optionalError.isEmpty()) {
        continue;
      }
      NullAwayError errorInRegion = (NullAwayError) optionalError.get();
      Set<MethodRewriteChange> changesForRegion = NO_ACTION;
      if (!safeRegions.isEmpty()) {
        // First try to fix by safe regions if exists.
        changesForRegion = gpt.fixDereferenceErrorBySafeRegions(errorInRegion, safeRegions);
      }
      if (changesForRegion.isEmpty()) {
        // If no safe region found, of no fix found by safe regions, try to fix by precondition
        // check.
        changesForRegion =
            gpt.fixDereferenceErrorByAllRegions(errorInRegion, safeRegions, unsafeRegions);
      }
      if (!changesForRegion.isEmpty()) {
        changes.addAll(changesForRegion);
      }
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
   *  + Preconditions.checkArgument(coll != null, "expected coll to be nonnull here.");
   *  coll.deref();
   * }}
   * }</pre>
   *
   * @param error the error to fix.
   * @return a {@link MethodRewriteChange} that represents the code fix.
   */
  private Set<MethodRewriteChange> constructPreconditionCheckMethodRewriteForError(
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
    String preconditionStatement =
        String.format(
            "%sPreconditions.checkArgument(%s != null, \"expected %s to be nonnull here.\");\n",
            Utility.getLeadingWhitespace(lines[errorLine]), expression, expression);
    lines[errorLine] = preconditionStatement + lines[errorLine];
    MethodRewriteChange change =
        new MethodRewriteChange(
            new OnMethod(error.path, error.getRegion().clazz, error.getRegion().member),
            String.join("\n", lines),
            // Add the import required for Preconditions.
            Set.of(NullAway.PRECONDITION_NAME));
    return Set.of(change);
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
    // Build a context for prompt generation
    InvocationRecord record = invocationRecordRegistry.computeInvocationRecord(encClass, method, 2);
    int count = 0;
    while (count++ < 10) {
      String callContext = record.constructCallGraphContext(parser);
      Response methodNullability = gpt.checkIfMethodIsReturningNullable(method, callContext);
      if (!methodNullability.isSuccessFull()) {
        ImmutableSet<String> methods =
            methodNullability.getValuesFromTag("/response/methods", "method");
        if (methods.isEmpty()) {
          throw new IllegalStateException(
              "Could not determine the nullability of the parameter and did not ask for any methods declaration.");
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
}
