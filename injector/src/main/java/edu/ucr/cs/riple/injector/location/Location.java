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

package edu.ucr.cs.riple.injector.location;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import edu.ucr.cs.riple.injector.Helper;
import edu.ucr.cs.riple.injector.changes.Change;
import edu.ucr.cs.riple.injector.exceptions.TargetClassNotFound;
import edu.ucr.cs.riple.injector.modifications.Modification;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nullable;

/** Represents a location of an element in the source code. */
public abstract class Location {

  /** The type of the element. */
  public final LocationKind type;
  /** The flat name of enclosing class of the element. */
  public final String clazz;
  /** The path to the file containing the element. */
  public Path path;

  /**
   * Creates an instance of {@link Location} for a given type, path and class. This constructor is a
   * base class for all subclasses and must provide these values upon instantiation.
   *
   * @param type The type of the element.
   * @param path The path to the file containing the element.
   * @param clazz The flat name of the enclosing class of the element.
   */
  public Location(LocationKind type, Path path, String clazz) {
    this.type = type;
    this.clazz = clazz;
    this.path = path;
  }

  /**
   * Creates an instance of {@link Location} based on values written in a row of a TSV file. These
   * values should be in order of:
   *
   * <ol>
   *   <li>Element Kind
   *   <li>Fully qualified Class flat name
   *   <li>Method Signature
   *   <li>Parameter / Variable name
   *   <li>Index in the argument list (Applicable to only parameter types)
   *   <li>URI to file containing the target element
   * </ol>
   *
   * If Element Kind is {@code "null"}, {@code null} will be returned.
   *
   * @param values Array of values in the expected order described above.
   * @return Corresponding {@link Location} instance.
   */
  @Nullable
  public static Location createLocationFromArrayInfo(String[] values) {
    Preconditions.checkArgument(
        values.length >= 6,
        "Expected at least 6 arguments to create a Location instance but found: "
            + Arrays.toString(values));
    if (values[0] == null || values[0].equals("null")) {
      return null;
    }
    LocationKind type = LocationKind.getType(values[0]);
    Path path = Helper.deserializePath(values[5]);
    String clazz = values[1];
    switch (type) {
      case FIELD:
        return new OnField(path, clazz, Sets.newHashSet(values[3]));
      case METHOD:
        return new OnMethod(path, clazz, values[2]);
      case PARAMETER:
        return new OnParameter(path, clazz, values[2], Integer.parseInt(values[4]));
    }
    throw new RuntimeException("Cannot reach this statement, values: " + Arrays.toString(values));
  }

  /**
   * Applies the change to the element in this location.
   *
   * @param members The list of members of the enclosing class of the target element.
   * @param change The change to be applied on the target element.
   * @return The modification that should be applied on the source file.
   */
  @Nullable
  protected abstract Modification applyToMember(
      NodeList<BodyDeclaration<?>> members, Change change);

  /**
   * Applies the change to the target element on the given compilation unit tree.
   *
   * @param tree CompilationUnit Tree to locate the target element.
   * @param change Change to be applied on the target element.
   * @return The modification that should be applied on the source file.
   */
  @Nullable
  public Modification apply(CompilationUnit tree, Change change) {
    NodeList<BodyDeclaration<?>> clazz;
    try {
      clazz = Helper.getTypeDeclarationMembersByFlatName(tree, this.clazz);
    } catch (TargetClassNotFound notFound) {
      System.err.println(notFound.getMessage());
      return null;
    }
    return applyToMember(clazz, change);
  }

  /**
   * If this location is of kind {@link LocationKind#METHOD}, calls the consumer on the location.
   *
   * @param consumer The consumer to be called.
   */
  public void ifMethod(Consumer<OnMethod> consumer) {}

  /**
   * If this location is of kind {@link LocationKind#PARAMETER}, calls the consumer on the location.
   *
   * @param consumer The consumer to be called.
   */
  public void ifParameter(Consumer<OnParameter> consumer) {}
  /**
   * If this location is of kind {@link LocationKind#FIELD}, calls the consumer on the location.
   *
   * @param consumer The consumer to be called.
   */
  public void ifField(Consumer<OnField> consumer) {}

  /**
   * Returns downcast of this instance to {@link OnField} if this location is of kind {@link
   * LocationKind#FIELD}, Otherwise, returns null.
   *
   * @return The {@link OnField} instance of this location if it is of kind {@link
   *     LocationKind#FIELD}, null otherwise.
   */
  public OnField toField() {
    if (this instanceof OnField) {
      return (OnField) this;
    }
    return null;
  }

  /**
   * Returns downcast of this instance to {@link OnMethod} if this location is of kind {@link
   * LocationKind#METHOD}, Otherwise, returns null.
   *
   * @return The {@link OnMethod} instance of this location if it is of kind {@link
   *     LocationKind#METHOD}, null otherwise.
   */
  public OnMethod toMethod() {
    if (this instanceof OnMethod) {
      return (OnMethod) this;
    }
    // If location is of kind PARAMETER, toMethod will return the location of the enclosing method
    // of the parameter.
    if (this instanceof OnParameter) {
      return new OnMethod(path, clazz, toParameter().method);
    }
    return null;
  }

  /**
   * Returns downcast of this instance to {@link OnParameter} if this location is of kind {@link
   * LocationKind#PARAMETER}, Otherwise, returns null.
   *
   * @return The {@link OnParameter} instance of this location if it is of kind {@link
   *     LocationKind#PARAMETER}, null otherwise.
   */
  public OnParameter toParameter() {
    if (this instanceof OnParameter) {
      return (OnParameter) this;
    }
    return null;
  }

  /**
   * Returns true if this location is of kind {@link LocationKind#METHOD}.
   *
   * @return true if this location is of kind {@link LocationKind#METHOD}.
   */
  public boolean isOnMethod() {
    return false;
  }

  /**
   * Returns true if this location is of kind {@link LocationKind#FIELD}.
   *
   * @return true if this location is of kind {@link LocationKind#FIELD}.
   */
  public boolean isOnField() {
    return false;
  }

  /**
   * Returns true if this location is of kind {@link LocationKind#PARAMETER}.
   *
   * @return true if this location is of kind {@link LocationKind#PARAMETER}.
   */
  public boolean isOnParameter() {
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Location)) {
      return false;
    }
    Location other = (Location) o;
    return type == other.type && clazz.equals(other.clazz);
  }

  /**
   * Applies a visitor to this location.
   *
   * @param <R> the return type of the visitor's methods
   * @param <P> the type of the additional parameter to the visitor's methods
   * @param v the visitor operating on this type
   * @param p additional parameter to the visitor
   * @return a visitor-specified result
   */
  public abstract <R, P> R accept(LocationVisitor<R, P> v, P p);

  @Override
  public int hashCode() {
    return Objects.hash(type, clazz);
  }

  /**
   * Returns the fully qualified class name of the target element.
   *
   * @return Fully qualified class name of the target element.
   */
  public String getClazz() {
    return clazz;
  }
}
