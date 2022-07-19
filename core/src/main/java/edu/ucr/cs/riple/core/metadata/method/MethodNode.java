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

package edu.ucr.cs.riple.core.metadata.method;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MethodNode {
  public List<Integer> children;
  public Integer parent;
  public Integer id;
  public String method;
  public String clazz;
  public int size;
  public boolean[] parameterAnnotationFlags;
  public boolean hasNullableAnnotation;
  public String uri;

  public MethodNode(
      int id,
      String clazz,
      String method,
      List<Integer> children,
      boolean[] parameterAnnotationFlags,
      int parent,
      boolean hasNullableAnnotation,
      String uri) {
    this.id = id;
    this.clazz = clazz;
    this.method = method;
    this.children = children;
    this.parameterAnnotationFlags = parameterAnnotationFlags;
    this.parent = parent;
    this.hasNullableAnnotation = hasNullableAnnotation;
    this.uri = uri;
  }

  public MethodNode() {}

  void fillInformation(
      Integer id,
      String clazz,
      String method,
      Integer parent,
      int size,
      boolean[] annotFlags,
      boolean hasNullableAnnotation,
      String uri) {
    this.parent = parent;
    this.id = id;
    this.method = method;
    this.clazz = clazz;
    this.size = size;
    this.parameterAnnotationFlags = annotFlags;
    this.hasNullableAnnotation = hasNullableAnnotation;
    this.uri = uri;
  }

  void addChild(Integer id) {
    if (children == null) {
      children = new ArrayList<>();
    }
    children.add(id);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MethodNode)) return false;
    MethodNode that = (MethodNode) o;
    return Objects.equals(children, that.children)
        && Objects.equals(parent, that.parent)
        && Objects.equals(id, that.id)
        && Objects.equals(method, that.method)
        && Objects.equals(clazz, that.clazz)
        && Objects.equals(uri, that.uri);
  }

  @Override
  public int hashCode() {
    return Objects.hash(method, clazz);
  }
}
