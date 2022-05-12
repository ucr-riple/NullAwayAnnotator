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
import edu.ucr.cs.riple.core.metadata.field.FieldDeclarationAnalysis;
import edu.ucr.cs.riple.injector.Change;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.LocationType;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.injector.location.OnMethod;
import edu.ucr.cs.riple.injector.location.OnParameter;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.json.simple.JSONObject;

public class Fix extends Hashable {

  public final Change change;
  public final LocationType type;
  public final String uri;
  public final String annotation;
  public final Set<String> reasons;

  public Fix(Change change, String reason, String encClass, String endMethod) {
    this.change = change;
    this.annotation = change.annotation;
    this.uri = change.location.uri;
    this.type = change.location.type;
    this.encClass = encClass;
    this.encMethod = endMethod;
    this.reasons = reason != null ? Sets.newHashSet(reason) : new HashSet<>();
  }

  public static Factory<Fix> factory(FieldDeclarationAnalysis analysis) {
    return infos -> {
      Location location = Location.createLocationFromArrayInfo(infos);
      location.ifField(
          onField -> {
            Set<String> variables =
                analysis.getInLineMultipleFieldDeclarationsOnField(
                    onField.clazz, onField.variables);
            onField.variables.addAll(variables);
          });
      return new Fix(new Change(location, infos[7], true), infos[6], infos[8], infos[9]);
    };
  }

  public boolean isOnMethod() {
    return change.location.type.equals(LocationType.METHOD);
  }

  public OnMethod toMethod() {
    return change.location.toMethod();
  }

  public boolean isOnParameter() {
    return change.location.type.equals(LocationType.PARAMETER);
  }

  public OnParameter toParameter() {
    return change.location.toParameter();
  }

  public boolean isOnField() {
    return change.location.type.equals(LocationType.FIELD);
  }

  public OnField toField() {
    return change.location.toField();
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
    if (!isOnMethod()) {
      return false;
    }
    OnMethod m = toMethod();
    String methodName = m.method.substring(0, m.method.indexOf("("));
    int lastIndex = m.clazz.lastIndexOf(".");
    String className = lastIndex < 0 ? m.clazz : m.clazz.substring(lastIndex + 1);
    return methodName.equals(className);
  }
}
