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

package test.depb;

import test.target.Foo;

public class DepB {

  Foo foo = new Foo();

  public void run() {
    exec1(foo.returnNullableBad(0));
    exec2(foo.returnNullableBad(0));
  }

  public void exec1(Object obj) {
    System.out.println(obj);
  }

  public void exec2(Object obj) {
    System.out.println(obj);
    System.out.println(exec3(foo.returnNullableBad(0)));
  }

  public Object exec3(Object obj) {
    if (obj == null) {
      return foo.returnNullableBad(0);
    }
    return obj;
  }

  public Object exec4() {
    return foo.returnNullableGood(0);
  }
}
