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

package edu.ucr.cs.riple.core.registries.region;

import edu.ucr.cs.riple.injector.location.OnClass;
import edu.ucr.cs.riple.injector.util.ASTUtils;
import edu.ucr.cs.riple.scanner.generatedcode.SourceType;
import java.util.Objects;

/**
 * Class for denoting a region in source code. A region can be either a method body or class field
 * initialization body.
 */
public class Region {
  /**
   * Symbol of the region representative. If region is a class static initialization region, member
   * value will be String of {@code "null"} not {@code null}.
   */
  public final String member;

  /** Fully qualified name of the enclosing class of the region. */
  public final String clazz;

  /**
   * Type of region. If region exists in source code, this value is {@code "SOURCE"}, otherwise it
   * will be the name of the processor created this region. (e.g. {"LOMBOK"}).
   */
  public final SourceType sourceType;

  public final Type type;

  /** Different types of code segments for a region. */
  public enum Type {
    METHOD,
    FIELD,
    CONSTRUCTOR,
    INIT_BLOCK
  }

  public Region(String encClass, String encMember, SourceType sourceType) {
    this.clazz = encClass == null ? "null" : encClass;
    this.member = encMember == null ? "null" : encMember;
    this.type = getType(encClass, member);
    this.sourceType = sourceType;
  }

  public Region(String encClass, String encMember) {
    this(encClass, encMember, SourceType.SOURCE);
  }

  /**
   * Initializes {@link Region#type} based on the string representation of regionMember.
   *
   * @param regionClass Symbol of the region class in string.
   * @param regionMember Symbol of the region representative in string.
   * @return The corresponding Type.
   */
  public static Type getType(String regionClass, String regionMember) {
    if (regionMember.equals("null")) {
      return Type.INIT_BLOCK;
    }
    if (regionMember.contains("(")) {
      return ASTUtils.extractCallableName(regionMember).equals(ASTUtils.simpleName(regionClass))
          ? Type.CONSTRUCTOR
          : Type.METHOD;
    }
    return Type.FIELD;
  }

  /**
   * Checks if region targets a method body.
   *
   * @return true, if region is targeting a method body.
   */
  public boolean isOnMethod() {
    return type.equals(Type.METHOD);
  }

  /**
   * Checks if region targets a constructor body.
   *
   * @return true, if region is targeting a constructor body.
   */
  public boolean isOnConstructor() {
    return type.equals(Type.CONSTRUCTOR);
  }

  /**
   * Checks if region targets a method or constructor body.
   *
   * @return true, if region is targeting a method or constructor body.
   */
  public boolean isOnCallable() {
    return isOnConstructor() || isOnMethod();
  }

  /**
   * Checks if region targets a field declaration.
   *
   * @return true, if region is targeting a field declaration.
   */
  public boolean isOnField() {
    return type.equals(Type.FIELD);
  }

  /**
   * Checks if region targets a static or instance initialization block.
   *
   * @return true, if region is targeting an initialization block.
   */
  public boolean isOnInitializationBlock() {
    return type.equals(Type.INIT_BLOCK);
  }

  /**
   * Checks if region is inside an anonymous class.
   *
   * @return true, if region is inside an anonymous class.
   */
  public boolean isInAnonymousClass() {
    return OnClass.isAnonymousClassFlatName(clazz);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Region)) {
      return false;
    }
    Region region = (Region) o;
    return Objects.equals(member, region.member) && Objects.equals(clazz, region.clazz);
  }

  @Override
  public int hashCode() {
    return Objects.hash(member, clazz);
  }

  @Override
  public String toString() {
    return "class='" + clazz + '\'' + ", member='" + member;
  }
}
