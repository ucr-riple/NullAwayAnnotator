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

package edu.ucr.cs.riple.core.registries.method;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.OnMethod;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Container class to store method's information in {@link MethodRegistry} tree. */
public class MethodRecord {

  /** Set of children's id. */
  public Set<Integer> children;
  /** Parent's node id. */
  public Integer parent;
  /** A unique id for method across all methods. */
  public Integer id;
  /** Location of the containing method. */
  public OnMethod location;
  /** Set of annotations on the method return type or the method itself */
  public ImmutableSet<String> annotations;
  /** Visibility of the method. */
  public Visibility visibility;
  /** Is true if the method has non-primitive return. */
  public boolean hasNonPrimitiveReturn;
  /** Is true if the method is a constructor. */
  public boolean isConstructor;

  public static final MethodRecord TOP = top();

  /** Visibility of method. */
  public enum Visibility {
    PUBLIC,
    PRIVATE,
    PACKAGE,
    PROTECTED;

    /**
     * Parses a string representation of visibility to an {@link Visibility} instance.
     *
     * @param value Visibility value in string.
     * @return Corresponding visibility instance.
     */
    public static MethodRecord.Visibility parse(String value) {
      String toLower = value.toLowerCase();
      switch (toLower) {
        case "public":
          return MethodRecord.Visibility.PUBLIC;
        case "private":
          return MethodRecord.Visibility.PRIVATE;
        case "protected":
          return MethodRecord.Visibility.PROTECTED;
        case "package":
          return MethodRecord.Visibility.PACKAGE;
        default:
          throw new IllegalArgumentException("Unknown visibility type: " + value);
      }
    }
  }

  /**
   * Creates a singleton instance of top object. Top is root of the {@link MethodRegistry} tree.
   *
   * @return The top Node.
   */
  private static MethodRecord top() {
    if (TOP == null) {
      MethodRecord node = new MethodRecord(0);
      node.fillInformation(null, -1, ImmutableSet.of(), "private", false, false);
      return node;
    }
    return TOP;
  }

  /**
   * Creates a MethodRecord with a unique id.
   *
   * @param id A unique id.
   */
  public MethodRecord(int id) {
    this.id = id;
  }

  /**
   * Initializes this instance's values.
   *
   * @param location Location of containing method.
   * @param parent Parent's id.
   * @param annotations Set of annotations on method return type.
   * @param visibility Visibility of this method.
   * @param hasNonPrimitiveReturn True, if it has a non-primitive return.
   * @param isConstructor True, if it is a constructor.
   */
  void fillInformation(
      OnMethod location,
      Integer parent,
      ImmutableSet<String> annotations,
      String visibility,
      boolean hasNonPrimitiveReturn,
      boolean isConstructor) {
    this.parent = parent;
    this.location = location;
    this.annotations = annotations;
    this.visibility = Visibility.parse(visibility);
    this.hasNonPrimitiveReturn = hasNonPrimitiveReturn;
    this.isConstructor = isConstructor;
  }

  /**
   * Adds a child to the list of children.
   *
   * @param id Child id.
   */
  void addChild(Integer id) {
    if (children == null) {
      children = new HashSet<>();
    }
    children.add(id);
  }

  public boolean isNonTop() {
    return !Objects.equals(this.id, TOP.id);
  }

  /**
   * Checks if method is public with non-primitive return type.
   *
   * @return true if method is public with non-primitive return type and false otherwise.
   */
  public boolean isPublicMethodWithNonPrimitiveReturnType() {
    return hasNonPrimitiveReturn && visibility.equals(MethodRecord.Visibility.PUBLIC);
  }

  /**
   * Returns true if method has nullable annotation on the return type.
   *
   * @return True if method has nullable annotation on the return type.
   */
  public boolean hasNullableAnnotation() {
    return annotations != null && annotations.stream().anyMatch(MethodRecord::isNullableAnnotation);
  }

  /**
   * Returns true if the given annotation should be acknowledged as a nullable annotation. The check
   * is based on the fully qualified name of the annotation.
   *
   * @param annot Fully qualified name of the annotation to check.
   * @return True if the given annotation should be acknowledged as a nullable annotation.
   */
  private static boolean isNullableAnnotation(String annot) {
    return annot.endsWith(".Nullable")
        || annot.endsWith(".checkerframework.checker.nullness.compatqual.NullableDecl")
        // matches javax.annotation.CheckForNull and
        // edu.umd.cs.findbugs.annotations.CheckForNull
        || annot.endsWith(".CheckForNull");
  }

  /**
   * Returns location of the method.
   *
   * @return Location of the method.
   */
  public Location toLocation() {
    return location;
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
    return Objects.equals(children, that.children)
        && Objects.equals(parent, that.parent)
        && Objects.equals(id, that.id)
        && Objects.equals(location, that.location);
  }

  /**
   * Calculates hash. This method is used outside this class to calculate the expected hash based on
   * instance's properties value if the actual instance is not available.
   *
   * @param method Method signature.
   * @param clazz Fully qualified name of the containing class.
   * @return Expected hash.
   */
  public static int hash(String method, String clazz) {
    return Objects.hash(method, clazz);
  }

  @Override
  public int hashCode() {
    return hash(location.method, location.clazz);
  }
}
