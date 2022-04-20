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
public class Location {
  public final String annotation;
  public final String method;
  public final String variable;
  public final String location;
  public final String clazz;
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

  public Location(
      String annotation,
      String method,
      String variable,
      String location,
      String clazz,
      String uri,
      String inject) {
    this.annotation = annotation;
    this.method = method;
    this.variable = variable;
    this.location = location;
    this.clazz = clazz;
    this.uri = uri;
    this.inject = inject;
    this.pkg = this.clazz.contains(".") ? this.clazz.substring(0, this.clazz.lastIndexOf(".")) : "";
  }

  public static List<Location> createFromJson(JSONObject locationJson, boolean deep) {
    List<Location> ans = new ArrayList<>();
    Location l = createFromJson(locationJson);
    ans.add(l);
    if (deep && locationJson.containsKey("tree")) {
      JSONArray followups = (JSONArray) locationJson.get("tree");
      followups.forEach(o -> ans.add(createFromJson((JSONObject) o)));
    }
    return ans;
  }

  public static Location createFromJson(JSONObject locationJson) {
    String uri = locationJson.get(KEYS.URI.label).toString();
    String file = "file:/";
    if (uri.contains(file)) uri = uri.substring(uri.indexOf(file) + file.length());
    String clazz = locationJson.get(KEYS.CLASS.label).toString();
    Location location =
        new Location(
            locationJson.get(KEYS.ANNOTATION.label).toString(),
            locationJson.get(KEYS.METHOD.label).toString(),
            locationJson.get(KEYS.PARAM.label).toString(),
            locationJson.get(KEYS.LOCATION.label).toString(),
            clazz,
            uri,
            locationJson.get(KEYS.INJECT.label).toString());
    if (locationJson.get(KEYS.INDEX.label) != null) {
      location.index = locationJson.get(KEYS.INDEX.label).toString();
    }
    return location;
  }

  @Override
  public String toString() {
    return location + " " + clazz + " " + method + " " + variable;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Location)) return false;
    Location other = (Location) o;
    return Objects.equals(annotation, other.annotation)
        && Objects.equals(method, other.method)
        && Objects.equals(variable, other.variable)
        && Objects.equals(location, other.location)
        && Objects.equals(clazz, other.clazz);
  }

  @Override
  public int hashCode() {
    return Objects.hash(annotation, method, variable, location, clazz);
  }

  public JSONObject getJson() {
    JSONObject res = new JSONObject();
    res.put(KEYS.CLASS.label, clazz);
    res.put(KEYS.METHOD.label, method);
    res.put(KEYS.PARAM.label, variable);
    res.put(KEYS.LOCATION.label, location);
    res.put(KEYS.PKG.label, pkg);
    res.put(KEYS.ANNOTATION.label, annotation);
    res.put(KEYS.INJECT.label, inject);
    res.put(KEYS.URI.label, uri);
    res.put(KEYS.INDEX.label, index);
    return res;
  }

  public Location duplicate() {
    Location location =
        new Location(annotation, method, variable, this.location, clazz, uri, inject);
    location.index = index;
    location.pkg = pkg;
    return location;
  }

  public static Location fromCSVLine(String line, String delimiter) {
    return fromArrayInfo(line.split(delimiter));
  }

  public static Location fromArrayInfo(String[] infos) {
    Location location =
        new Location(infos[7], infos[2], infos[3], infos[0], infos[1], infos[5], "true");
    location.pkg = location.clazz.substring(0, location.clazz.lastIndexOf("."));
    location.index = infos[4];
    return location;
  }
}
