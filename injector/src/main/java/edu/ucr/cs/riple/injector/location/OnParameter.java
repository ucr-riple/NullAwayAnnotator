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

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.Parameter;
import edu.ucr.cs.riple.injector.Helper;
import edu.ucr.cs.riple.injector.changes.Change;
import edu.ucr.cs.riple.injector.modifications.Modification;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.annotation.Nullable;

/**
 * Represents a location for parameter element. This location is used to apply changes to a
 * parameter.
 */
public class OnParameter extends Location {

  /** Enclosing Method location of the parameter. */
  public final OnMethod enclosingMethod;
  /** Index of the parameter in the method signature. */
  public final int index;

  public OnParameter(Path path, String clazz, String method, int index) {
    super(LocationKind.PARAMETER, path, clazz);
    this.enclosingMethod = new OnMethod(path, clazz, method);
    this.index = index;
  }

  public OnParameter(String path, String clazz, String method, int index) {
    this(Helper.deserializePath(path), clazz, method, index);
  }

  @Override
  @Nullable
  protected Modification applyToMember(NodeList<BodyDeclaration<?>> members, Change change) {
    final AtomicReference<Modification> ans = new AtomicReference<>();
    members.forEach(
        bodyDeclaration ->
            bodyDeclaration.ifCallableDeclaration(
                callableDeclaration -> {
                  if (ans.get() != null) {
                    // already found the member.
                    return;
                  }
                  if (enclosingMethod.matcher.matchesCallableDeclaration(callableDeclaration)) {
                    NodeList<?> params = callableDeclaration.getParameters();
                    if (index < params.size()) {
                      if (params.get(index) != null) {
                        Node param = params.get(index);
                        if (param instanceof Parameter) {
                          ans.set(change.visit((Parameter) param));
                        }
                      }
                    }
                  }
                }));
    return ans.get();
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
    if (this == o) {
      return true;
    }
    if (!(o instanceof OnParameter)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    OnParameter other = (OnParameter) o;
    return super.equals(other)
        && enclosingMethod.equals(other.enclosingMethod)
        && index == other.index;
  }

  @Override
  public <R, P> R accept(LocationVisitor<R, P> v, P p) {
    return v.visitParameter(this, p);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), enclosingMethod, index);
  }

  @Override
  public String toString() {
    return "OnParameter{"
        + "class='"
        + clazz
        + '\''
        + ", method='"
        + enclosingMethod
        + '\''
        + ", index="
        + index
        + '}';
  }
}
