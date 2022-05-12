package edu.ucr.cs.riple.injector.location;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.google.common.base.Preconditions;
import edu.ucr.cs.riple.injector.Helper;
import java.util.Arrays;
import java.util.Collections;
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
        return new OnField(uri, clazz, Collections.singleton(infos[3]));
      case METHOD:
        return new OnMethod(uri, clazz, infos[2]);
      case PARAMETER:
        return new OnParameter(uri, clazz, infos[2], infos[3], Integer.parseInt(infos[4]));
    }
    throw new RuntimeException("Cannot reach this statement, infos: " + Arrays.toString(infos));
  }

  public abstract Location duplicate();

  protected abstract boolean applyToMember(
      NodeList<BodyDeclaration<?>> clazz, String annotation, boolean inject);

  protected abstract void fillJsonInformation(JSONObject res);

  public boolean apply(CompilationUnit tree, String annotation, boolean inject) {
    NodeList<BodyDeclaration<?>> clazz =
        Helper.getClassOrInterfaceOrEnumDeclarationMembersByFlatName(tree, this.clazz);
    if (clazz == null) {
      return false;
    }
    return applyToMember(clazz, annotation, inject);
  }

  @SuppressWarnings("unchecked")
  public JSONObject getJson() {
    JSONObject res = new JSONObject();
    res.put(KEYS.CLASS, clazz);
    res.put(KEYS.KIND, type);
    res.put(KEYS.URI, uri);
    fillJsonInformation(res);
    return res;
  }

  protected static void applyAnnotation(
      NodeWithAnnotations<?> node, String annotName, boolean inject) {
    final String annotSimpleName = Helper.simpleName(annotName);
    NodeList<AnnotationExpr> annots = node.getAnnotations();
    boolean exists =
        annots
            .stream()
            .anyMatch(
                annot -> {
                  String thisAnnotName = annot.getNameAsString();
                  return thisAnnotName.equals(annotName) || thisAnnotName.equals(annotSimpleName);
                });
    if (inject && !exists) {
      node.addMarkerAnnotation(annotSimpleName);
    }
    if (!inject) {
      annots.removeIf(
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
    return type == other.type && clazz.equals(other.clazz) && uri.equals(other.uri);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, clazz, uri);
  }
}
