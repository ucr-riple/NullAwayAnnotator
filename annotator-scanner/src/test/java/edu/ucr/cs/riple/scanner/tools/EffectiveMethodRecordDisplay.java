/*
 * MIT License
 *
 * Copyright (c) 2025 Nima Karimipour
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

public class EffectiveMethodRecordDisplay implements Display {

  private final String clazz;
  private final String method;
  private final String parameter;
  private final String index;

  public EffectiveMethodRecordDisplay(String clazz, String method, String parameter, String index) {
    this.clazz = clazz;
    this.method = method;
    this.parameter = parameter;
    this.index = index;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof EffectiveMethodRecordDisplay)) {
      return false;
    }
    EffectiveMethodRecordDisplay that = (EffectiveMethodRecordDisplay) o;
    return Objects.equals(clazz, that.clazz)
        && Objects.equals(method, that.method)
        && Objects.equals(parameter, that.parameter)
        && Objects.equals(index, that.index);
  }

  @Override
  public int hashCode() {
    return Objects.hash(clazz, method, parameter, index);
  }

  @Override
  public String toString() {
    return "{"
        + "clazz='"
        + clazz
        + '\''
        + ", method='"
        + method
        + '\''
        + ", parameter='"
        + parameter
        + '\''
        + ", index='"
        + index
        + '\''
        + '}';
  }
}
