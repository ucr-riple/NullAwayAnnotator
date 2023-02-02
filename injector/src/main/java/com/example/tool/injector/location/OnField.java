/*
 * MIT License
 *
 * Copyright (c) 2020 anonymous
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

package com.example.tool.injector.location;

import com.example.tool.injector.modifications.Modification;
import com.github.javaparser.Range;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.example.tool.injector.Helper;
import com.example.tool.injector.changes.Change;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

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
    super(LocationType.FIELD, path, clazz);
    this.variables = variables;
  }

  public OnField(String path, String clazz, Set<String> variables) {
    this(Helper.deserializePath(path), clazz, variables);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void fillJsonInformation(JSONObject res) {
    JSONArray fields = new JSONArray();
    fields.addAll(variables);
    res.put(KEYS.VARIABLES, fields);
  }

  @Override
  protected Modification applyToMember(NodeList<BodyDeclaration<?>> clazz, Change change) {
    final AtomicReference<Modification> ans = new AtomicReference<>();
    clazz.forEach(
        bodyDeclaration ->
            bodyDeclaration.ifFieldDeclaration(
                fieldDeclaration -> {
                  if (ans.get() != null) {
                    // already found the member.
                    return;
                  }
                  NodeList<VariableDeclarator> vars =
                      fieldDeclaration.asFieldDeclaration().getVariables();
                  for (VariableDeclarator v : vars) {
                    if (variables.contains(v.getName().toString())) {
                      Optional<Range> range = fieldDeclaration.getRange();
                      range.ifPresent(value -> ans.set(change.visit(fieldDeclaration, value)));
                      break;
                    }
                  }
                }));
    return ans.get();
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
  public int hashCode() {
    return Objects.hash(super.hashCode(), variables);
  }

  @Override
  public String toString() {
    return "OnField{" + "variables=" + variables + ", clazz=" + clazz + ", path=" + path + '}';
  }
}