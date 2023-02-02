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

package com.example.tool.core.metadata.method;

import com.example.tool.injector.location.OnMethod;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** Container class to store method's information in {@link MethodDeclarationTree} tree. */
public class MethodNode {

  /** Set of children's id. */
  public Set<Integer> children;
  /** Parent's node id. */
  public Integer parent;
  /** A unique id for method across all methods. */
  public Integer id;
  /** Location of the containing method. */
  public OnMethod location;
  /** Number of parameters. */
  public int numberOfParameters;
  /** Is true if the method is already annotated with {@code Nullable} annotation. */
  public boolean hasNullableAnnotation;
  /** Visibility of the method. */
  public Visibility visibility;
  /** Is true if the method has non-primitive return. */
  public boolean hasNonPrimitiveReturn;

  public static final MethodNode TOP = top();

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
    public static MethodNode.Visibility parse(String value) {
      String toLower = value.toLowerCase();
      switch (toLower) {
        case "public":
          return MethodNode.Visibility.PUBLIC;
        case "private":
          return MethodNode.Visibility.PRIVATE;
        case "protected":
          return MethodNode.Visibility.PROTECTED;
        case "package":
          return MethodNode.Visibility.PACKAGE;
        default:
          throw new IllegalArgumentException("Unknown visibility type: " + value);
      }
    }
  }

  /**
   * Creates a singleton instance of top object. Top is root of the {@link MethodDeclarationTree}
   * tree.
   *
   * @return The top Node.
   */
  private static MethodNode top() {
    if (TOP == null) {
      MethodNode node = new MethodNode(0);
      node.fillInformation(null, -1, 0, false, "private", false);
      return node;
    }
    return TOP;
  }

  /**
   * Creates a MethodNode with a unique id.
   *
   * @param id A unique id.
   */
  public MethodNode(int id) {
    this.id = id;
  }

  /**
   * Initializes this instance's values.
   *
   * @param location Location of containing method.
   * @param parent Parent's id.
   * @param numberOfParameters Number of parameters.
   * @param hasNullableAnnotation True, if it has Nullable Annotation.
   * @param visibility Visibility of this method.
   * @param hasNonPrimitiveReturn True, if it has a non-primitive return.
   */
  void fillInformation(
      OnMethod location,
      Integer parent,
      int numberOfParameters,
      boolean hasNullableAnnotation,
      String visibility,
      boolean hasNonPrimitiveReturn) {
    this.parent = parent;
    this.location = location;
    this.numberOfParameters = numberOfParameters;
    this.hasNullableAnnotation = hasNullableAnnotation;
    this.visibility = Visibility.parse(visibility);
    this.hasNonPrimitiveReturn = hasNonPrimitiveReturn;
  }

  /**
   * Adds a child to the list of children.
   *
   * @param id Child id.
   */
  void addChild(Integer id) {
    if (children == null) {
      children = new LinkedHashSet<>();
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
    return hasNonPrimitiveReturn && visibility.equals(MethodNode.Visibility.PUBLIC);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MethodNode)) {
      return false;
    }
    MethodNode that = (MethodNode) o;
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