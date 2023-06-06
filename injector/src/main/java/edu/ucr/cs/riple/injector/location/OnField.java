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

package edu.ucr.cs.riple.injector.location;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import edu.ucr.cs.riple.injector.Helper;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import org.json.simple.JSONObject;

/**
 * Represents a location for field element. This location is used to apply changes to a class field.
 */
public class OnField extends Location {

  /**
   * Set of field names. It is a set to support inline multiple field declarations. Please see the
   * example below:
   *
   * <ul>
   *   <li>Foo bar; -> variables = {"bar"}
   *   <li>Foo bar1, bar2; -> variables = {"bar1", "bar2"}
   * </ul>
   *
   * We do not split inline multiple field declarations on injections, any annotation targeting any
   * element in {@code variables} will be applied to the group.
   */
  public final Set<String> variables;

  public OnField(Path path, String clazz, Set<String> variables) {
    super(LocationKind.FIELD, path, clazz);
    // Check that the set is not empty and does not contain null elements.
    Preconditions.checkArgument(!variables.isEmpty());
    Preconditions.checkArgument(variables.stream().noneMatch(Objects::isNull));
    this.variables = variables;
  }

  public OnField(String path, String clazz, Set<String> variables) {
    this(Helper.deserializePath(path), clazz, variables);
  }

  public OnField(JSONObject json) {
    super(LocationKind.FIELD, json);
    String fieldName = (String) json.get("field");
    if (fieldName == null) {
      throw new IllegalArgumentException("Field name cannot be null, " + json);
    }
    this.variables = Sets.newHashSet(fieldName);
  }

  @Override
  public void ifField(Consumer<OnField> consumer) {
    consumer.accept(this);
  }

  @Override
  public boolean isOnField() {
    return true;
  }

  public boolean isOnFieldWithName(String fieldName) {
    return variables.contains(fieldName);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof OnField)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    OnField other = (OnField) o;
    return super.equals(other) && !Collections.disjoint(variables, other.variables);
  }

  @Override
  public <R, P> R accept(LocationVisitor<R, P> v, P p) {
    return v.visitField(this, p);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), variables);
  }

  @Override
  public String toString() {
    return "OnField{" + "variables=" + variables + ", clazz='" + clazz + '\'' + '}';
  }
}
