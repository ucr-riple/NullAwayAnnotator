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

package edu.ucr.cs.riple.core.metadata.index;

import edu.ucr.cs.riple.injector.Location;
import java.util.Objects;
import org.json.simple.JSONObject;

public class Fix extends Hashable {

  public final Location location;
  public final String clazz;
  public final String method;
  public final String kind;
  public final String variable;
  public final String uri;
  public final String index;
  public final String annotation;
  public int referred;

  public Fix(Location location, String encClass, String endMethod) {
    this.location = location;
    this.annotation = location.annotation;
    this.uri = location.uri;
    this.clazz = location.clazz;
    this.method = location.method;
    this.variable = location.variable;
    this.kind = location.kind;
    this.index = location.index;
    this.encClass = encClass;
    this.encMethod = endMethod;
    this.referred = 0;
  }

  public Fix(String[] infos) {
    this(Location.fromArrayInfo(infos), infos[8], infos[9]);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Fix)) return false;
    Fix fix = (Fix) o;
    return location.equals(fix.location);
  }

  @Override
  public int hashCode() {
    return Objects.hash(location);
  }

  public JSONObject getJson() {
    return location.getJson();
  }
}
