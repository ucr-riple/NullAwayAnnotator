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

public class OnParameter extends Location {
  public final String method;
  public final String parameter;
  public final int index;

  public OnParameter(String uri, String clazz, String method, String parameter, int index) {
    super(LocationType.PARAMETER, uri, clazz);
    this.method = method;
    this.parameter = parameter;
    this.index = index;
  }

  @Override
  public Location duplicate() {
    return new OnParameter(clazz, uri, method, parameter, index);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void fillJsonInformation(JSONObject res) {
    res.put(KEYS.METHOD, method);
    res.put(KEYS.PARAMETER, parameter);
    res.put(KEYS.INDEX, index);
  }

  @Override
  protected boolean applyToMember(
      NodeList<BodyDeclaration<?>> members, String annotation, boolean inject) {
    final boolean[] success = {false};
    members.forEach(
        bodyDeclaration ->
            bodyDeclaration.ifCallableDeclaration(
                callableDeclaration -> {
                  if (Helper.matchesCallableSignature(callableDeclaration, method)) {
                    for (Object p : callableDeclaration.getParameters()) {
                      if (p instanceof com.github.javaparser.ast.body.Parameter) {
                        com.github.javaparser.ast.body.Parameter param =
                            (com.github.javaparser.ast.body.Parameter) p;
                        if (param.getName().toString().equals(parameter)) {
                          applyAnnotation(param, annotation, inject);
                          success[0] = true;
                        }
                      }
                    }
                  }
                }));
    return success[0];
  }

  @Override
  public void ifParameter(Consumer<OnParameter> consumer) {
    consumer.accept(this);
  }

  @Override
  public boolean isOnParameter() {
    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof OnParameter)) return false;
    if (!super.equals(o)) return false;
    OnParameter other = (OnParameter) o;
    return super.equals(other) && method.equals(other.method) && parameter.equals(other.parameter);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), method, parameter);
  }
}
