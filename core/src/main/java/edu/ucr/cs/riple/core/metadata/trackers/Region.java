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

package edu.ucr.cs.riple.core.metadata.trackers;

import java.util.Objects;

/**
 * Class for denoting a region in source code. A region can be either a method body or class field
 * initialization body.
 */
public class Region {
  /**
   * Signature of the method where this region is enclosed. If region is a class field
   * initialization region, method value will be String of {@code "null"} not {@code null}.
   */
  public final String method;
  /** Fully qualified name of the enclosing class of the region. */
  public final String clazz;

  public Region(String method, String clazz) {
    this.method = method == null ? "null" : method;
    this.clazz = clazz == null ? "null" : clazz;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Region)) return false;
    Region region = (Region) o;
    return Objects.equals(method, region.method) && Objects.equals(clazz, region.clazz);
  }

  @Override
  public int hashCode() {
    return Objects.hash(method, clazz);
  }

  @Override
  public String toString() {
    return "class='" + clazz + '\'' + ", method='" + method;
  }
}
