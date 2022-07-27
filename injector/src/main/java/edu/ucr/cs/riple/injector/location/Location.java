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
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import edu.ucr.cs.riple.injector.Helper;
import edu.ucr.cs.riple.injector.exceptions.TargetClassNotFound;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import org.json.simple.JSONObject;

public abstract class Location {
  public final LocationType type;
  public final String clazz;
  public String uri;

  public enum KEYS {
    VARIABLES,
    PARAMETER,
    METHOD,
    KIND,
    CLASS,
    PKG,
    URI,
    INJECT,
    ANNOTATION,
    INDEX
  }

  public Location(LocationType type, String uri, String clazz) {
    this.type = type;
    this.clazz = clazz;
    this.uri = uri;
  }

  public static Location createLocationFromArrayInfo(String[] infos) {
    Preconditions.checkArgument(
        infos.length >= 6,
        "Expected at least 6 arguments to create a Location instance but found: "
            + Arrays.toString(infos));
    LocationType type = LocationType.getType(infos[0]);
    String uri = infos[5];
    String clazz = infos[1];
    switch (type) {
      case FIELD:
        return new OnField(uri, clazz, Sets.newHashSet(infos[3]));
      case METHOD:
        return new OnMethod(uri, clazz, infos[2]);
      case PARAMETER:
        return new OnParameter(uri, clazz, infos[2], Integer.parseInt(infos[4]));
    }
    throw new RuntimeException("Cannot reach this statement, infos: " + Arrays.toString(infos));
  }

  public abstract Location duplicate();

  protected abstract boolean applyToMember(
      NodeList<BodyDeclaration<?>> clazz, String annotation, boolean inject);

  protected abstract void fillJsonInformation(JSONObject res);

  /**
   * Applies the change to the target element on the given compilation unit tree.
   *
   * @param tree CompilationUnit Tree to locate the target element.
   * @param annotation Fully qualified name of the annotation.
   * @param inject If set to true, the given annotation will be added to the target element, and if
   *     set to false, the given annotation will be removed from the element.
   * @return true, if the change applied successfully.
   */
  public boolean apply(CompilationUnit tree, String annotation, boolean inject) {
    NodeList<BodyDeclaration<?>> clazz;
    try {
      clazz = Helper.getTypeDeclarationMembersByFlatName(tree, this.clazz);
    } catch (TargetClassNotFound notFound) {
      System.err.println(notFound.getMessage());
      return false;
    }
    return applyToMember(clazz, annotation, inject);
  }

  @SuppressWarnings("unchecked")
  public JSONObject getJson() {
    JSONObject res = new JSONObject();
    res.put(KEYS.CLASS, clazz);
    res.put(KEYS.KIND, type.toString());
    res.put(KEYS.URI, uri);
    fillJsonInformation(res);
    return res;
  }

  protected static void applyAnnotation(
      NodeWithAnnotations<?> node, String annotName, boolean inject) {
    final String annotSimpleName = Helper.simpleName(annotName);
    NodeList<AnnotationExpr> annotations = node.getAnnotations();
    boolean exists =
        annotations.stream()
            .anyMatch(
                annot -> {
                  String thisAnnotName = annot.getNameAsString();
                  return thisAnnotName.equals(annotName) || thisAnnotName.equals(annotSimpleName);
                });
    if (inject && !exists) {
      node.addMarkerAnnotation(annotSimpleName);
    }
    if (!inject) {
      annotations.removeIf(
          annot -> {
            String thisAnnotName = annot.getNameAsString();
            return thisAnnotName.equals(annotName) || thisAnnotName.equals(annotSimpleName);
          });
    }
  }

  public void ifMethod(Consumer<OnMethod> consumer) {}

  public void ifParameter(Consumer<OnParameter> consumer) {}

  public void ifField(Consumer<OnField> consumer) {}

  public OnField toField() {
    if (this instanceof OnField) {
      return (OnField) this;
    }
    return null;
  }

  public OnMethod toMethod() {
    if (this instanceof OnMethod) {
      return (OnMethod) this;
    }
    return null;
  }

  public OnParameter toParameter() {
    if (this instanceof OnParameter) {
      return (OnParameter) this;
    }
    return null;
  }

  public boolean isOnMethod() {
    return false;
  }

  public boolean isOnField() {
    return false;
  }

  public boolean isOnParameter() {
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Location)) return false;
    Location other = (Location) o;
    return type == other.type && clazz.equals(other.clazz);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, clazz);
  }
}
