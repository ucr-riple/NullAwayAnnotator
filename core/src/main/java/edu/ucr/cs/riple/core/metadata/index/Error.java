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
package edu.ucr.cs.riple.core.metadata.index;

import java.util.Objects;

/** Represents an error reported by NullAway. */
public class Error extends Enclosed {

  /** Error Type. */
  public final String messageType;
  /** Error message. */
  public final String message;

  /**
   * NullAway serializes error on TSV file, this constructor is called for each line of that file.
   *
   * @param values Values in row of a TSV file.
   */
  public Error(String[] values) {
    this(values[0], values[1], values[2], values[3]);
  }

  public Error(String messageType, String message, String encClass, String encMethod) {
    super(encClass, encMethod);
    this.messageType = messageType;
    this.message = message;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Error)) return false;
    Error error = (Error) o;
    return messageType.equals(error.messageType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(messageType);
  }
}
