/*
 * MIT License
 *
 * Copyright (c) 2023 Nima Karimipour
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

import edu.ucr.cs.riple.injector.Helper;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;
import org.json.simple.JSONObject;

/**
 * Represents a location for local variable element. This location is used to apply changes to a
 * local variable.
 *
 * <p>Annotations on local variables will be applied on the declaration and all its type arguments
 * if exists. For instance: {@code Foo<Bar, Map<Bar, Bar>> baz}
 *
 * <p>Will be annotated as: {@code @Annot Foo<@Annot Bar, @Annot Map<@Annot Bar, @Annot Bar>> baz}
 */
public class OnLocalVariable extends Location {

  /** Enclosing method of the target local variable. */
  public final OnMethod encMethod;
  /** Name of the local variable. */
  public final String varName;

  public OnLocalVariable(Path path, String clazz, String encMethod, String varName) {
    super(LocationKind.LOCAL_VARIABLE, path, clazz);
    this.encMethod = new OnMethod(path, clazz, encMethod);
    this.varName = varName;
  }

  public OnLocalVariable(String path, String clazz, String encMethod, String varName) {
    this(Helper.deserializePath(path), clazz, encMethod, varName);
  }

  public OnLocalVariable(JSONObject json) {
    super(LocationKind.LOCAL_VARIABLE, json);
    this.encMethod = new OnMethod(json);
    this.varName = (String) json.get("varName");
  }

  @Override
  public void ifLocalVariable(Consumer<OnLocalVariable> consumer) {
    consumer.accept(this);
  }

  @Override
  public boolean isOnLocalVariable() {
    return true;
  }

  @Override
  public boolean equals(Object o) {
    boolean ans = super.equals(o);
    if (!ans) {
      return false;
    }
    OnLocalVariable that = (OnLocalVariable) o;
    return this.encMethod.equals(that.encMethod) && this.varName.equals(that.varName);
  }

  @Override
  public <R, P> R accept(LocationVisitor<R, P> v, P p) {
    return v.visitLocalVariable(this, p);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), encMethod, varName);
  }

  @Override
  public String toString() {
    return "OnLocalVariable{"
        + "encMethod='"
        + encMethod
        + '\''
        + ", clazz='"
        + clazz
        + '\''
        + "varName='"
        + varName
        + '}';
  }
}
