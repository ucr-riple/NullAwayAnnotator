/*
 * MIT License
 *
 * Copyright (c) 2022 Nima Karimipour
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

public class MethodRecordDisplay implements Display {

  public final String id;
  public final String clazz;
  public final String symbol;
  public final String parent;
  public final String flags;
  public final String hasNullableAnnotation;
  public final String visibility;
  public final String hasNonPrimitiveReturn;
  public String uri;

  public MethodRecordDisplay(
      String id,
      String clazz,
      String symbol,
      String parent,
      String flags,
      String hasNullableAnnotation,
      String visibility,
      String hasNonPrimitiveReturn,
      String uri) {
    this.id = id;
    this.clazz = clazz;
    this.symbol = symbol;
    this.parent = parent;
    this.flags = flags;
    this.hasNullableAnnotation = hasNullableAnnotation;
    this.visibility = visibility;
    this.hasNonPrimitiveReturn = hasNonPrimitiveReturn;
    this.uri = uri;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MethodRecordDisplay)) {
      return false;
    }
    MethodRecordDisplay that = (MethodRecordDisplay) o;
    return Objects.equals(clazz, that.clazz)
        && Objects.equals(symbol, that.symbol)
        && Objects.equals(parent, that.parent)
        && Objects.equals(flags, that.flags)
        && Objects.equals(hasNullableAnnotation, that.hasNullableAnnotation)
        && Objects.equals(visibility, that.visibility)
        && Objects.equals(hasNonPrimitiveReturn, that.hasNonPrimitiveReturn)
        && Objects.equals(uri, that.uri);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        clazz,
        symbol,
        parent,
        flags,
        hasNullableAnnotation,
        visibility,
        hasNonPrimitiveReturn,
        uri);
  }

  @Override
  public String toString() {
    return "id='"
        + id
        + '\''
        + ", clazz='"
        + clazz
        + '\''
        + ", symbol='"
        + symbol
        + '\''
        + ", parent='"
        + parent
        + '\''
        + ", flags='"
        + flags
        + '\''
        + ", hasNullableAnnotation='"
        + hasNullableAnnotation
        + '\''
        + ", visibility='"
        + visibility
        + '\''
        + ", hasNonPrimitiveReturn='"
        + hasNonPrimitiveReturn
        + '\''
        + ", uri='"
        + uri
        + '\'';
  }
}
