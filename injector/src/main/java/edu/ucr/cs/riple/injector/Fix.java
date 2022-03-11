/*
 * Copyright (c) 2022 University of California, Riverside.
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
    INDEX("index");
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
    return fix;
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
        && Objects.equals(index, fix.index);
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
    return Objects.hash(annotation, method, param, location, className);
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
    res.put(KEYS.INDEX.label, index);
    return res;
  }

  public Fix duplicate() {
    Fix fix = new Fix(annotation, method, param, location, className, uri, inject);
    fix.index = index;
    fix.pkg = pkg;
    return fix;
  }

  public static Fix fromCSVLine(String line, String delimiter) {
    return fromArrayInfo(line.split(delimiter));
  }

  public static Fix fromArrayInfo(String[] infos) {
    Fix fix = new Fix(infos[7], infos[2], infos[3], infos[0], infos[1], infos[5], "true");
    fix.pkg = fix.className.substring(0, fix.className.lastIndexOf("."));
    fix.index = infos[4];
    return fix;
  }
}
