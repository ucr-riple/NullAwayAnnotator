/*
 * Copyright (c) 2022 University of California, Riverside.
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

package test.target;

public class Foo {
  Object field = new Object();

  // Making this method will resolve 5 errors here and will introduce 1 new error here and 8 new
  // errors on downstream dependencies.
  // dependencies.
  public Object returnNullableBad(int i) {
    // Just to create 5 places where it returns nullable, so making this method @Nullable will
    // resolve 5 NullAway errors
    if (i < 0) {
      return null;
    }
    if (i > 0 && i < 10) {
      return null;
    }
    if (i > 10 && i < 20) {
      return null;
    }
    if (i > 20 && i < 30) {
      return null;
    }
    if (i > 30 && i < 40) {
      return bar();
    }
    return null;
  }

  public void run() {
    this.field = returnNullableBad(0);
    // Just to make a new error for annotating bar() @Nullable.
    this.field = bar();
  }

  // Making this method will resolve 5 errors here and will introduce 1 error on downstream
  // dependencies.
  public Object returnNullableGood(int i) {
    // Just to create 5 places where it returns nullable.
    if (i < 0) {
      return null;
    }
    if (i > 0 && i < 10) {
      return null;
    }
    if (i > 10 && i < 20) {
      return null;
    }
    if (i > 20 && i < 30) {
      return null;
    }
    return null;
  }

  public Object bar() {
    return null;
  }
}
