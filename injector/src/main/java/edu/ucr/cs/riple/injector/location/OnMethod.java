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

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import edu.ucr.cs.riple.injector.Helper;
import java.util.Objects;
import java.util.function.Consumer;
import org.json.simple.JSONObject;

public class OnMethod extends Location {
  public final String method;

  public OnMethod(String uri, String clazz, String method) {
    super(LocationType.METHOD, uri, clazz);
    this.method = method;
  }

  @Override
  public Location duplicate() {
    return new OnMethod(clazz, uri, method);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void fillJsonInformation(JSONObject res) {
    res.put(KEYS.METHOD, method);
  }

  @Override
  protected boolean applyToMember(
      NodeList<BodyDeclaration<?>> clazz, String annotation, boolean inject) {
    final boolean[] success = {false};
    clazz.forEach(
        bodyDeclaration ->
            bodyDeclaration.ifCallableDeclaration(
                callableDeclaration -> {
                  if (Helper.matchesCallableSignature(callableDeclaration, method)) {
                    applyAnnotation(callableDeclaration, annotation, inject);
                    success[0] = true;
                  }
                }));
    if (!success[0]) {
      clazz.forEach(
          bodyDeclaration ->
              bodyDeclaration.ifAnnotationMemberDeclaration(
                  annotationMemberDeclaration -> {
                    if (annotationMemberDeclaration
                        .getNameAsString()
                        .equals(Helper.extractCallableName(method))) {
                      applyAnnotation(annotationMemberDeclaration, annotation, inject);
                      success[0] = true;
                    }
                  }));
    }
    return success[0];
  }

  @Override
  public void ifMethod(Consumer<OnMethod> consumer) {
    consumer.accept(this);
  }

  @Override
  public boolean isOnMethod() {
    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof OnMethod)) return false;
    OnMethod other = (OnMethod) o;
    return super.equals(other) && method.equals(other.method);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), method);
  }

  @Override
  public String toString() {
    return "OnMethod{" + "method='" + method + '\'' + ", clazz='" + clazz + '\'' + '}';
  }
}
