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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.code.Symbol;
import edu.ucr.cs.riple.scanner.ScannerContext;
import edu.ucr.cs.riple.scanner.Serializer;
import edu.ucr.cs.riple.scanner.SymbolUtil;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Modifier;

/** Container class to store information regarding a method in source code. */
public class MethodRecord {

  /** Symbol of containing method. */
  private final Symbol.MethodSymbol symbol;

  /** Symbol of the enclosing class for the method. */
  private final Symbol.ClassSymbol clazz;

  /** Path to file containing the source file. */
  private URI uri;

  /** Unique id assigned to this method across all visited methods. */
  private final int id;

  /**
   * Set of annotations on the method return type or the method itself. If the method has no
   * annotations, then this field will be empty and not {@code null}.
   */
  private ImmutableSet<? extends AnnotationMirror> annotations;

  /**
   * Flag value for parameters annotations. If {@code annotFlags[j]} is {@code true} then the
   * parameter at index {@code j} has a {@code @Nullable} annotation.
   */
  private Boolean[] parameterAnnotationFlags;

  /** ID of the closest super method. */
  private int parentID;

  /** Delimiter used to separate annotations in the serialized output. */
  public static final String ANNOTATION_DELIMITER = ",";

  private MethodRecord(Symbol.MethodSymbol method, ScannerContext context) {
    this.id = context.getNextMethodId();
    this.symbol = method;
    this.clazz = (method != null) ? method.enclClass() : null;
    this.parentID = 0;
    context.visitMethod(this);
  }

  /**
   * Creates a {@link MethodRecord} instance if not visited, otherwise, it will return the
   * corresponding instance.
   *
   * @param method Method symbol.
   * @param context Scanner context.
   * @return The corresponding {@link MethodRecord} instance.
   */
  public static MethodRecord findOrCreate(Symbol.MethodSymbol method, ScannerContext context) {
    Symbol.ClassSymbol clazz = method.enclClass();
    Optional<MethodRecord> optionalMethodInfo =
        context
            .getVisitedMethodsWithHashHint(hash(method))
            .filter(
                methodRecord ->
                    methodRecord.symbol.equals(method) && methodRecord.clazz.equals(clazz))
            .findAny();
    return optionalMethodInfo.orElseGet(() -> new MethodRecord(method, context));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MethodRecord)) {
      return false;
    }
    MethodRecord that = (MethodRecord) o;
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
    MethodRecord superMethodRecord = findOrCreate(superMethod, context);
    this.parentID = superMethodRecord.id;
  }

  @Override
  public String toString() {
    Preconditions.checkArgument(symbol != null, "Should not be null at this point.");
    Path path = Serializer.pathToSourceFileFromURI(uri);
    return String.join(
        "\t",
        String.valueOf(id),
        (clazz != null ? clazz.flatName() : "null"),
        Serializer.serializeSymbol(symbol),
        String.valueOf(parentID),
        Arrays.toString(parameterAnnotationFlags),
        annotations.stream()
            // only interested in the annotation type for now.
            .map(annot -> annot.getAnnotationType().toString())
            .collect(Collectors.joining(ANNOTATION_DELIMITER)),
        getVisibilityOfMethod(),
        String.valueOf(!symbol.getReturnType().isPrimitiveOrVoid()),
        // for build systems that might return null for bytecodes.
        (path != null ? path.toString() : "null"));
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
        "flags",
        "annotations",
        "visibility",
        "non-primitive-return",
        "path");
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

  /** Initializes the set of annotations for this method. */
  public void collectMethodAnnotations() {
    this.annotations =
        SymbolUtil.getAllAnnotations(this.symbol).collect(ImmutableSet.toImmutableSet());
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
   * potential {@link MethodRecord} object without creating the {@link MethodRecord} instance.
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
