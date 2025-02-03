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

package edu.ucr.cs.riple.core.util;

import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.type.Type;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.registries.region.Region;
import edu.ucr.cs.riple.injector.Injector;
import edu.ucr.cs.riple.injector.SourceCode;
import edu.ucr.cs.riple.injector.location.OnMethod;
import edu.ucr.cs.riple.injector.util.SignatureMatcher;
import edu.ucr.cs.riple.injector.util.TypeUtils;
import java.nio.file.Path;

/** AnnotatorNullabilityUtil class for AST operations. */
public class ASTParser {

  private final Config config;
  private final AnnotationExpr nullableAnnotationExpression;

  public ASTParser(Config config) {
    this.config = config;
    // find simple name of the nullable annotation
    String nullableAnnotationSimpleName =
        config.nullableAnnot.substring(config.nullableAnnot.lastIndexOf('.') + 1);
    this.nullableAnnotationExpression = new MarkerAnnotationExpr(nullableAnnotationSimpleName);
  }

  /**
   * Checks if a member is the {@link Object#equals(Object)} method of the Object class.
   *
   * @param member the member to check.
   * @return true if the member is the {@link Object#equals(Object)} method of the Object class.
   */
  public static boolean isObjectEqualsMethod(String member) {
    if (!SignatureMatcher.isCallableDeclaration(member)) {
      return false;
    }
    SignatureMatcher matcher = new SignatureMatcher(member);
    if (!matcher.callableName.equals("equals")) {
      return false;
    }
    if (matcher.parameterTypes.size() != 1) {
      return false;
    }
    return matcher.parameterTypes.get(0).equals("java.lang.Object");
  }

  /**
   * Checks if a member is the {@link Object#toString()} method of the Object class.
   *
   * @param member the member to check.
   * @return true if the member is the {@link Object#toString()} method of the Object class.
   */
  public static boolean isObjectToStringMethod(String member) {
    if (!SignatureMatcher.isCallableDeclaration(member)) {
      return false;
    }
    SignatureMatcher matcher = new SignatureMatcher(member);
    if (!matcher.callableName.equals("toString")) {
      return false;
    }
    return matcher.parameterTypes.isEmpty();
  }

  /**
   * Checks if a member is the {@link Object#hashCode()} method of the Object class.
   *
   * @param member the member to check.
   * @return true if the member is the {@link Object#hashCode()} method of the Object class.
   */
  public static boolean isObjectHashCodeMethod(String member) {
    if (!SignatureMatcher.isCallableDeclaration(member)) {
      return false;
    }
    SignatureMatcher matcher = new SignatureMatcher(member);
    if (!matcher.callableName.equals("hashCode")) {
      return false;
    }
    return matcher.parameterTypes.isEmpty();
  }

  /**
   * Returns the source code of a region.
   *
   * @param path the path of the source file.
   * @param region the region to get the source code of.
   * @return the source code of the region.
   */
  public SourceCode getRegionSourceCode(Path path, Region region) {
    return Injector.getMethodSourceCode(path, region.clazz, region.member, config.languageLevel);
  }

  /**
   * Returns the callable declaration of a method.
   *
   * @param onMethod the method to get the callable declaration of.
   * @return the callable declaration of a method.
   */
  public CallableDeclaration<?> getCallableDeclaration(OnMethod onMethod) {
    return getCallableDeclaration(onMethod.path, onMethod.clazz, onMethod.method);
  }

  /**
   * Returns the callable declaration of a class.
   *
   * @param path the path of the source file.
   * @param clazz the enclosing class.
   * @param member Method signature.
   * @return the callable declaration of a class.
   */
  public CallableDeclaration<?> getCallableDeclaration(Path path, String clazz, String member) {
    return Injector.getCallableDeclaration(path, clazz, member, config.languageLevel);
  }

  /**
   * Checks if a method has a nullable return type. The {@code @Nullable} annotation can be either a
   * type-use or a declaration annotation.
   *
   * @param declaration the method to check.
   * @return true if the method has a nullable return type.
   */
  public boolean isMethodWithNullableReturn(CallableDeclaration<?> declaration) {
    boolean onNode = TypeUtils.isAnnotatedWith(declaration, nullableAnnotationExpression);
    if (onNode) {
      return true;
    }
    Type returnType = TypeUtils.getTypeFromNode(declaration);
    return TypeUtils.isAnnotatedWith(returnType, nullableAnnotationExpression);
  }
}
