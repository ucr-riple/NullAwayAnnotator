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

import com.google.common.collect.Sets;
import edu.ucr.cs.riple.injector.Change;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.json.simple.JSONObject;

public class Fix extends Hashable {

  public final Change change;
  public final String clazz;
  public final String method;
  public final String kind;
  public final String variable;
  public final String uri;
  public final String index;
  public final String annotation;
  public final Set<String> reasons;
  public final Set<Fix> aliases;

  public Fix(Change change, String reason, String encClass, String endMethod) {
    this.change = change;
    this.annotation = change.annotation;
    this.uri = change.uri;
    this.clazz = change.clazz;
    this.method = change.method;
    this.variable = change.variable;
    this.kind = change.kind;
    this.index = change.index;
    this.encClass = encClass;
    this.encMethod = endMethod;
    this.reasons = reason != null ? Sets.newHashSet(reason) : new HashSet<>();
    this.aliases = new HashSet<>();
  }

  public Fix(String[] infos) {
    this(Change.fromArrayInfo(infos, infos[7]), infos[6], infos[8], infos[9]);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Fix)) return false;
    Fix fix = (Fix) o;
    return change.equals(fix.change);
  }

  @Override
  public int hashCode() {
    return Objects.hash(change);
  }

  public JSONObject getJson() {
    return change.getJson();
  }

  public boolean isModifyingConstructor() {
    String methodName = method.substring(0, method.indexOf("("));
    int lastIndex = clazz.lastIndexOf(".");
    String className = lastIndex < 0 ? clazz : clazz.substring(lastIndex + 1);
    return methodName.equals(className);
  }
}
