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

// todo we have to store more information for dif to work in bank

public class Error extends Hashable {
  public final String messageType;
  public final String message;

  public Error(String[] infos) {
    this(infos[0], infos[1], infos[2], infos[3]);
  }

  public Error(String messageType, String message, String clazz, String method) {
    this.messageType = messageType;
    this.message = message;
    this.method = method;
    this.clazz = clazz;
  }

  @Override
  public String toString() {
    return "messageType='"
        + messageType
        + '\''
        + ", message='"
        + message
        + '\''
        + ", clazz='"
        + clazz
        + '\''
        + ", method='"
        + method;
  }
}
