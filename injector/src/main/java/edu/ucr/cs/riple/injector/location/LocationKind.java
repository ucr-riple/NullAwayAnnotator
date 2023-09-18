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

/** Represents the type of the location. */
public enum LocationKind {
  LOCAL_VARIABLE,
  CLASS,
  FIELD,
  METHOD,
  POLY_METHOD,
  PARAMETER;

  /**
   * Returns the type of the location based on the string representation.
   *
   * @param type the string representation of the type.
   * @return the type of the location.
   */
  public static LocationKind getType(String type) {
    if (type.equalsIgnoreCase("local_variable")) {
      return LOCAL_VARIABLE;
    }
    if (type.equalsIgnoreCase("field")) {
      return FIELD;
    }
    if (type.equalsIgnoreCase("method")) {
      return METHOD;
    }
    if (type.equalsIgnoreCase("parameter")) {
      return PARAMETER;
    }
    if (type.equalsIgnoreCase("class")) {
      return CLASS;
    }
    if (type.equalsIgnoreCase("poly_method")) {
      return POLY_METHOD;
    }
    throw new UnsupportedOperationException("Cannot detect type: " + type);
  }
}
