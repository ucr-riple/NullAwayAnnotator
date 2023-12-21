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

  // This is the field which will be accessed directly by the downstream dependencies
  // Making it nullable will decrease the number of errors on target
  // but it will create unresolvable errors on downstream dependencies as well.
  // Annotator should be able to detect this and leave f untouched.
  public Object f;

  // This field is safe to be annotated as @Nullable. No use in downstream dependencies. But it trigger making f @Nullable as well.
  // Annotator should be able to detect this and leave f1 untouched.
  public Object f1;

  // This field is safe to be annotated as @Nullable. The resulting error on downstream dependencies can be fixed by making f3 @Nullable.
  public Object f2;

  // This field receives @Nullable flowing back from downstream dependencies through f2.
  public Object f3 = new Object();

  public void propagateF1ToF() {
    this.f = f1;
  }
}
