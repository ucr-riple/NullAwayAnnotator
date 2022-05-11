package edu.ucr.cs.riple.injector.location;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import edu.ucr.cs.riple.injector.Helper;
import org.json.simple.JSONObject;

public abstract class Location {
  public final String kind;
  public final String clazz;
  public final String uri;

  public Location(String kind, String clazz, String uri) {
    this.kind = kind;
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
    res.put(KEYS.KIND, kind);
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
}
