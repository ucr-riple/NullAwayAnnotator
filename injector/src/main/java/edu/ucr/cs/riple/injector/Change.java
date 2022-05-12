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

import com.github.javaparser.ast.CompilationUnit;
import edu.ucr.cs.riple.injector.location.Location;
import java.util.Objects;
import org.json.simple.JSONObject;

@SuppressWarnings("unchecked")
public class Change {
  public final Location location;
  public final String annotation;
  public final boolean inject;

  public Change(Location location, String annotation, boolean inject) {
    this.annotation = annotation;
    this.location = location;
    this.inject = inject;
  }

  public boolean apply(CompilationUnit tree) {
    return this.location.apply(tree, annotation, inject);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Change)) return false;
    Change other = (Change) o;
    return Objects.equals(location, other.location) && Objects.equals(annotation, other.annotation);
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
    return new Change(location.duplicate(), annotation, inject);
  }

  /**
   * Creates a Change instance from array of info, this array info is typically coming from a line
   * in a TSV file with the format: (location class method param index uri annotation inject)
   *
   * @param infos array of info (a line from TSV file).
   * @return a Change instance corresponding values in infos.
   */
  public static Change fromArrayInfo(String[] infos) {
    Location location = Location.createLocationFromArrayInfo(infos);
    return new Change(location, infos[6], Boolean.parseBoolean(infos[7]));
  }
}
