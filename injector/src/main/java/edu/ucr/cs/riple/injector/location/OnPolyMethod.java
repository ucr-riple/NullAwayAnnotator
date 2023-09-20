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

import com.google.common.collect.ImmutableList;
import edu.ucr.cs.riple.injector.Helper;
import edu.ucr.cs.riple.injector.SignatureMatcher;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.json.simple.JSONObject;

public class OnPolyMethod extends Location {

  /** Indices of the arguments in the method which determines the method return type. */
  public final ImmutableList<Integer> indices;
  /** Method signature of the target element. */
  public final String method;
  /**
   * Matcher for the method signature. Method signature is given as a string, this matcher is used
   * to match the target.
   */
  public final SignatureMatcher matcher;

  public OnPolyMethod(Path path, String clazz, String method, List<Integer> indices) {
    super(LocationKind.POLY_METHOD, path, clazz);
    this.method = method;
    this.indices = ImmutableList.copyOf(indices);
    this.matcher = new SignatureMatcher(method);
  }

  public OnPolyMethod(String path, String clazz, String method, List<Integer> indices) {
    this(Helper.deserializePath(path), clazz, method, indices);
  }

  public OnPolyMethod(JSONObject json) {
    super(LocationKind.POLY_METHOD, json);
    this.method = (String) json.get("method");
    this.matcher = new SignatureMatcher(method);
    this.indices =
        ((List<Long>) json.get("arguments"))
            .stream().map(Long::intValue).collect(ImmutableList.toImmutableList());
  }

  @Override
  public <R, P> R accept(LocationVisitor<R, P> v, P p) {
    return v.visitPolyMethod(this, p);
  }

  @Override
  public void ifPolyMethod(Consumer<OnPolyMethod> consumer) {
    consumer.accept(this);
  }

  @Override
  public boolean isOnPolyMethod() {
    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof OnPolyMethod)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    OnPolyMethod that = (OnPolyMethod) o;
    return Objects.equals(indices, that.indices) && Objects.equals(method, that.method);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), indices, method);
  }
}
