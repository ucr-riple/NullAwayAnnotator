/*
 * MIT License
 *
 * Copyright (c) 2020 anonymous
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

package com.example.too.scanner.out;

import com.google.common.base.Preconditions;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.code.Symbol;
import com.example.too.scanner.Config;
import com.example.too.scanner.ScannerContext;
import com.example.too.scanner.SymbolUtil;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.Modifier;

/** Container class to store information regarding a method in source code. */
public class MethodInfo {

  /** Symbol of containing method. */
  private final Symbol.MethodSymbol symbol;
  /** Symbol of the enclosing class for the method. */
  private final Symbol.ClassSymbol clazz;
  /** Path to file containing the source file. */
  private URI uri;
  /** Unique id assigned to this method across all visited methods. */
  private final int id;
  /**
   * Flag value for parameters annotations. If {@code annotFlags[j]} is {@code true} then the
   * parameter at index {@code j} has a {@code @Nullable} annotation.
   */
  private Boolean[] parameterAnnotationFlags;
  /** If {@code true} the method has a {@code @Nullable} annotation on it's return type. */
  private boolean hasNullableOnReturnType;
  /** ID of the closest super method. */
  private int parentID;

  private MethodInfo(Symbol.MethodSymbol method, ScannerContext context) {
    this.id = context.getNextMethodId();
    this.symbol = method;
    this.clazz = (method != null) ? method.enclClass() : null;
    this.parentID = 0;
    context.visitMethod(this);
  }

  /**
   * Creates a {@link MethodInfo} instance if not visited, otherwise, it will return the
   * corresponding instance.
   *
   * @param method Method symbol.
   * @param context Scanner context.
   * @return The corresponding {@link MethodInfo} instance.
   */
  public static MethodInfo findOrCreate(Symbol.MethodSymbol method, ScannerContext context) {
    Symbol.ClassSymbol clazz = method.enclClass();
    Optional<MethodInfo> optionalMethodInfo =
        context
            .getVisitedMethodsWithHashHint(hash(method))
            .filter(
                methodInfo -> methodInfo.symbol.equals(method) && methodInfo.clazz.equals(clazz))
            .findAny();
    return optionalMethodInfo.orElseGet(() -> new MethodInfo(method, context));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MethodInfo)) {
      return false;
    }
    MethodInfo that = (MethodInfo) o;
    return symbol.equals(that.symbol) && clazz.equals(that.clazz);
  }

  @Override
  public int hashCode() {
    return hash(symbol);
  }

  /**
   * Locates the id of the closes super method and initializes it.
   *
   * @param state Error prone visitor state.
   * @param context Scanner context.
   */
  public void findParent(VisitorState state, ScannerContext context) {
    Symbol.MethodSymbol superMethod =
        SymbolUtil.getClosestOverriddenMethod(symbol, state.getTypes());
    if (superMethod == null || superMethod.toString().equals("null")) {
      this.parentID = 0;
      return;
    }
    MethodInfo superMethodInfo = findOrCreate(superMethod, context);
    this.parentID = superMethodInfo.id;
  }

  @Override
  public String toString() {
    Preconditions.checkArgument(symbol != null, "Should not be null at this point.");
    return String.join(
        "\t",
        String.valueOf(id),
        (clazz != null ? clazz.flatName() : "null"),
        symbol.toString(),
        String.valueOf(parentID),
        String.valueOf(symbol.getParameters().size()),
        Arrays.toString(parameterAnnotationFlags),
        String.valueOf(hasNullableOnReturnType),
        getVisibilityOfMethod(),
        String.valueOf(!symbol.getReturnType().isPrimitiveOrVoid()),
        // for build systems that might return null for bytecodes.
        (uri != null ? uri.toString() : "null"));
  }

  /**
   * Returns header of the file where all these instances will be serialized.
   *
   * @return Header of target file.
   */
  public static String header() {
    return String.join(
        "\t",
        "id",
        "class",
        "method",
        "parent",
        "size",
        "flags",
        "nullable",
        "visibility",
        "non-primitive-return",
        "uri");
  }

  /**
   * Setter for parameter annotation flags.
   *
   * @param annotFlags Parameter annotation flags.
   */
  public void setAnnotationParameterFlags(List<Boolean> annotFlags) {
    if (annotFlags == null) {
      annotFlags = Collections.emptyList();
    }
    this.parameterAnnotationFlags = new Boolean[annotFlags.size()];
    this.parameterAnnotationFlags = annotFlags.toArray(this.parameterAnnotationFlags);
  }

  /**
   * Sets return type annotation. Checks if method has {@code @Nullable} annotation on its return
   * type.
   *
   * @param config Scanner config.
   */
  public void setReturnTypeAnnotation(Config config) {
    this.hasNullableOnReturnType = SymbolUtil.hasNullableAnnotation(this.symbol, config);
  }

  /**
   * Return string value of visibility
   *
   * @return "public" if public, "private" if private, "protected" if protected and "package" if the
   *     method has package visibility.
   */
  private String getVisibilityOfMethod() {
    Set<Modifier> modifiers = symbol.getModifiers();
    if (modifiers.contains(Modifier.PUBLIC)) {
      return "public";
    }
    if (modifiers.contains(Modifier.PRIVATE)) {
      return "private";
    }
    if (modifiers.contains(Modifier.PROTECTED)) {
      return "protected";
    }
    return "package";
  }

  /**
   * Calculates hash. The hash is calculated based on a {@link
   * com.sun.tools.javac.code.Symbol.MethodSymbol} instance since we want to predict the hash of a
   * potential {@link MethodInfo} object without creating the {@link MethodInfo} instance.
   *
   * @param method Method Symbol.
   * @return Expected hash.
   */
  public static int hash(Symbol method) {
    return Objects.hash(method, method.enclClass());
  }

  /**
   * Sets uri based on the visitor state.
   *
   * @param state VisitorState instance.
   */
  public void setURI(VisitorState state) {
    CompilationUnitTree tree = state.getPath().getCompilationUnit();
    this.uri = tree.getSourceFile() != null ? tree.getSourceFile().toUri() : null;
  }
}
