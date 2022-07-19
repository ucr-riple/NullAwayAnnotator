/*
 * MIT License
 *
 * Copyright (c) 2020 Nima Karimipour
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

package edu.ucr.cs.riple.scanner.out;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import edu.ucr.cs.riple.scanner.Config;
import edu.ucr.cs.riple.scanner.SymbolUtil;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import javax.lang.model.element.Modifier;

public class MethodInfo {
  public final Symbol.MethodSymbol symbol;
  public final Symbol.ClassSymbol clazz;
  final int id;

  private Boolean[] annotFlags;
  private String[] parameterNames;
  private boolean hasNullableAnnotation;
  private int parent = -1;
  private static int LAST_ID = 0;
  private static final Set<MethodInfo> discovered = new HashSet<>();
  private URI uri;

  private MethodInfo(Symbol.MethodSymbol method) {
    this.id = ++LAST_ID;
    this.symbol = method;
    this.clazz = (method != null) ? method.enclClass() : null;
    discovered.add(this);
  }

  public static MethodInfo findOrCreate(Symbol.MethodSymbol method) {
    Symbol.ClassSymbol clazz = method.enclClass();
    Optional<MethodInfo> optionalMethodInfo =
        discovered.stream()
            .filter(
                methodInfo -> methodInfo.symbol.equals(method) && methodInfo.clazz.equals(clazz))
            .findAny();
    return optionalMethodInfo.orElseGet(() -> new MethodInfo(method));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MethodInfo)) return false;
    MethodInfo that = (MethodInfo) o;
    return symbol.equals(that.symbol) && clazz.equals(that.clazz);
  }

  @Override
  public int hashCode() {
    return Objects.hash(symbol, clazz);
  }

  public void findParent(VisitorState state) {
    Symbol.MethodSymbol superMethod =
        SymbolUtil.getClosestOverriddenMethod(symbol, state.getTypes());
    if (superMethod == null || superMethod.toString().equals("null")) {
      this.parent = 0;
      return;
    }
    MethodInfo superMethodInfo = findOrCreate(superMethod);
    this.parent = superMethodInfo.id;
  }

  @Override
  public String toString() {
    Preconditions.checkArgument(symbol != null, "Should not be null at this point.");
    return String.join(
        "\t",
        String.valueOf(id),
        (clazz != null ? clazz.flatName() : "null"),
        symbol.toString(),
        String.valueOf(parent),
        String.valueOf(symbol.getParameters().size()),
        Arrays.toString(annotFlags),
        String.valueOf(hasNullableAnnotation),
        getVisibilityOfMethod(),
        Arrays.toString(parameterNames),
        uri.getPath());
  }

  public static String header() {
    return "id"
        + "\t"
        + "class"
        + "\t"
        + "method"
        + "\t"
        + "parent"
        + "\t"
        + "size"
        + "\t"
        + "flags"
        + "\t"
        + "nullable"
        + "\t"
        + "visibility"
        + "\t"
        + "parameters"
        + "\t"
        + "uri";
  }

  public void setParamAnnotations(List<Boolean> annotFlags) {
    if (annotFlags == null) {
      annotFlags = Collections.emptyList();
    }
    this.annotFlags = new Boolean[annotFlags.size()];
    this.annotFlags = annotFlags.toArray(this.annotFlags);
  }

  public void setAnnotation(Config config) {
    this.hasNullableAnnotation = SymbolUtil.hasNullableAnnotation(this.symbol, config);
  }

  public void setURI(URI toUri) {
    this.uri = toUri;
  }

  public void setParameterNames(List<? extends VariableTree> parameters) {
    this.parameterNames =
        parameters.stream()
            .map(
                new Function<VariableTree, String>() {
                  @Override
                  public @Nullable String apply(@Nullable VariableTree variableTree) {
                    if (variableTree == null) {
                      return null;
                    }
                    return variableTree.getName().toString();
                  }
                })
            .toArray(String[]::new);
  }

  /**
   * Return string value of visibility
   *
   * @return "public" if public, "private" if private and "package" if the method has package
   *     visibility.
   */
  private String getVisibilityOfMethod() {
    Set<Modifier> modifiers = symbol.getModifiers();
    if (modifiers.contains(Modifier.PUBLIC)) {
      return "public";
    }
    if (modifiers.contains(Modifier.PRIVATE)) {
      return "private";
    }
    return "package";
  }
}
