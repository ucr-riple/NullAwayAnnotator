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

import java.util.Objects;

import edu.ucr.cs.riple.injector.location.Location;
import org.json.simple.JSONObject;

@SuppressWarnings("unchecked")
public class Change {
  public final Location location;
  public final String annotation;
  public final boolean inject;

  public Change(
      Location location,
      String annotation,
      boolean inject) {
    this.annotation = annotation;
    this.location = location;
    this.inject = inject;
  }

  public static Change createFromJson(JSONObject locationJson) {
    String uri = locationJson.get(Location.KEYS.URI).toString();
    String file = "file:/";
    if (uri.contains(file)) uri = uri.substring(uri.indexOf(file) + file.length());
    String clazz = locationJson.get(Location.KEYS.CLASS).toString();
    Change location =
        new Change(
            locationJson.get(Location.KEYS.ANNOTATION).toString(),
            locationJson.get(Location.KEYS.METHOD).toString(),
            locationJson.get(Location.KEYS.VARIABLES).toString(),
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
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Change)) return false;
    Change other = (Change) o;
    return Objects.equals(location, other.location)
        && Objects.equals(annotation, other.annotation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(annotation, location);
  }

  public JSONObject getJson() {
    JSONObject res = new JSONObject();
    res.put("LOCATION", location.getJson());
    res.put("INJECT", inject);
    res.put("ANNOTATION", annotation);
    return res;
  }

  public Change duplicate() {
    return new Change(location.duplicate(), annotation,, inject);
  }

  public static Change fromArrayInfo(String[] infos, String annotation) {
    Change location =
        new Change(annotation, infos[2], infos[3], infos[0], infos[1], infos[5], "true");
    location.index = infos[4];
    return location;
  }
}
