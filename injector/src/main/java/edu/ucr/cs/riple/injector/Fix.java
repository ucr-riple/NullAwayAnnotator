package edu.ucr.cs.riple.injector;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

@SuppressWarnings("unchecked")
public class Fix {
  public final String annotation;
  public final String method;
  public final String param;
  public final String location;
  public final String className;
  public final String inject;
  public String uri;
  public String compulsory;
  public String index;
  public String pkg;
  public int referred;

  public enum KEYS {
    PARAM("param"),
    METHOD("method"),
    LOCATION("location"),
    CLASS("class"),
    PKG("pkg"),
    URI("uri"),
    INJECT("inject"),
    ANNOTATION("annotation"),
    INDEX("index"),
    COMPULSORY("compulsory");
    public final String label;

    KEYS(String label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return "KEYS{" + "label='" + label + '\'' + '}';
    }
  }

  public Fix(
      String annotation,
      String method,
      String param,
      String location,
      String className,
      String uri,
      String inject) {
    this.annotation = annotation;
    this.method = method;
    this.param = param;
    this.location = location;
    this.className = className;
    this.uri = uri;
    this.inject = inject;
    this.pkg =
        this.className.contains(".")
            ? this.className.substring(0, this.className.lastIndexOf("."))
            : "";
  }

  public static List<Fix> createFromJson(JSONObject fixJson, boolean deep) {
    List<Fix> ans = new ArrayList<>();
    Fix fix = createFromJson(fixJson);
    ans.add(fix);
    if (deep && fixJson.containsKey("followups")) {
      JSONArray followups = (JSONArray) fixJson.get("followups");
      followups.forEach(o -> ans.add(createFromJson((JSONObject) o)));
    }
    return ans;
  }

  public static Fix createFromJson(JSONObject fixJson) {
    String uri = fixJson.get(KEYS.URI.label).toString();
    String file = "file:/";
    if (uri.contains(file)) uri = uri.substring(uri.indexOf(file) + file.length());
    String clazz = fixJson.get(KEYS.CLASS.label).toString();
    Fix fix =
        new Fix(
            fixJson.get(KEYS.ANNOTATION.label).toString(),
            fixJson.get(KEYS.METHOD.label).toString(),
            fixJson.get(KEYS.PARAM.label).toString(),
            fixJson.get(KEYS.LOCATION.label).toString(),
            clazz,
            uri,
            fixJson.get(KEYS.INJECT.label).toString());
    if (fixJson.get(KEYS.INDEX.label) != null) {
      fix.index = fixJson.get(KEYS.INDEX.label).toString();
    }
    if (fixJson.get(KEYS.COMPULSORY.label) != null) {
      fix.compulsory = fixJson.get(KEYS.COMPULSORY.label).toString();
    }
    return fix;
  }

  public String display() {
    return "\n  {"
        + "\n\tannotation='"
        + annotation
        + '\''
        + ", \n\tmethod='"
        + method
        + '\''
        + ", \n\tparam='"
        + param
        + '\''
        + ", \n\tlocation='"
        + location
        + '\''
        + ", \n\tclassName='"
        + className
        + '\''
        + ", \n\tpkg='"
        + pkg
        + '\''
        + ", \n\tinject='"
        + inject
        + '\''
        + ", \n\turi='"
        + uri
        + '\''
        + ", \n\tindex='"
        + index
        + '\''
        + ", \n\tcompulsory='"
        + compulsory
        + '\''
        + "\n  }\n";
  }

  @Override
  public String toString() {
    return location + " " + className + " " + method + " " + param;
  }

  public boolean deepEquals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Fix)) return false;
    Fix fix = (Fix) o;
    return Objects.equals(annotation, fix.annotation)
        && Objects.equals(method, fix.method)
        && Objects.equals(param, fix.param)
        && Objects.equals(location, fix.location)
        && Objects.equals(className, fix.className)
        && Objects.equals(pkg, fix.pkg)
        && Objects.equals(inject, fix.inject)
        && Objects.equals(uri, fix.uri)
        && Objects.equals(index, fix.index)
        && Objects.equals(compulsory, fix.compulsory);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Fix)) return false;
    Fix fix = (Fix) o;
    return Objects.equals(annotation, fix.annotation)
        && Objects.equals(method, fix.method)
        && Objects.equals(param, fix.param)
        && Objects.equals(location, fix.location)
        && Objects.equals(className, fix.className);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        annotation, method, param, location, className, pkg, inject, uri, compulsory, index);
  }

  public JSONObject getJson() {
    JSONObject res = new JSONObject();
    res.put(KEYS.CLASS.label, className);
    res.put(KEYS.METHOD.label, method);
    res.put(KEYS.PARAM.label, param);
    res.put(KEYS.LOCATION.label, location);
    res.put(KEYS.PKG.label, pkg);
    res.put(KEYS.ANNOTATION.label, annotation);
    res.put(KEYS.INJECT.label, inject);
    res.put(KEYS.URI.label, uri);
    res.put(KEYS.COMPULSORY.label, compulsory);
    res.put(KEYS.INDEX.label, index);
    return res;
  }

  public Fix duplicate() {
    Fix fix = new Fix(annotation, method, param, location, className, uri, inject);
    fix.index = index;
    fix.compulsory = compulsory;
    fix.pkg = pkg;
    return fix;
  }

  public static Fix fromCSVLine(String line, String delimiter) {
    return fromArrayInfo(line.split(delimiter));
  }

  public static Fix fromArrayInfo(String[] infos) {
    Fix fix = new Fix(infos[8], infos[3], infos[4], infos[0], infos[2], infos[6], infos[10]);
    fix.pkg = infos[1];
    fix.compulsory = infos[9];
    fix.index = infos[5];
    return fix;
  }
}
