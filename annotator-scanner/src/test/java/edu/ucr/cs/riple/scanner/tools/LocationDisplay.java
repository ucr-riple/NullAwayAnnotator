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

package edu.ucr.cs.riple.scanner.tools;

import java.util.Objects;

/**
 * Helper class to represent a suggested fix contents in a test case's (expected or actual) output.
 */
public class LocationDisplay implements Display {

  public final String kind;
  public final String method;
  public final String param;
  public final String index;
  public final String className;
  public String uri;

  public LocationDisplay(
      String kind, String clazz, String method, String param, String index, String uri) {
    this.kind = kind;
    this.method = method;
    this.param = param;
    this.index = index;
    this.className = clazz;
    this.uri = uri;
  }

  @Override
  public String toString() {
    return "\n  LocationDisplay{"
        + ", \n\tkind='"
        + kind
        + '\''
        + "'\n\tmethod='"
        + method
        + '\''
        + ", \n\tparam='"
        + param
        + '\''
        + ", \n\tclassName='"
        + className
        + '\''
        + ", \n\turi='"
        + uri
        + '\''
        + "\n  }\n";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof LocationDisplay)) {
      return false;
    }
    LocationDisplay fix = (LocationDisplay) o;
    return Objects.equals(method, fix.method)
        && Objects.equals(param, fix.param)
        && Objects.equals(kind, fix.kind)
        && Objects.equals(className, fix.className)
        && Objects.equals(uri, fix.uri);
  }

  @Override
  public int hashCode() {
    return Objects.hash(method, param, kind, className, uri);
  }
}
