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
import org.json.simple.JSONObject;

public abstract class Location {
  public final LocationType type;
  public final String clazz;
  public String uri;

  public Location(LocationType type, String uri, String clazz) {
    this.type = type;
    this.clazz = clazz;
    this.uri = uri;
  }

  public abstract Location duplicate();

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

  protected abstract void fillJsonInformation(JSONObject res);

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

  protected abstract boolean applyToMember(
      NodeList<BodyDeclaration<?>> clazz, String annotation, boolean inject);

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
        return new Field(uri, clazz, Collections.singleton(infos[3]));
      case METHOD:
        return new Method(uri, clazz, infos[2]);
      case PARAMETER:
        return new Parameter(uri, clazz, infos[2], infos[3], Integer.parseInt(infos[4]));
    }
    throw new RuntimeException("Cannot reach this statement, infos: " + Arrays.toString(infos));
  }
}
