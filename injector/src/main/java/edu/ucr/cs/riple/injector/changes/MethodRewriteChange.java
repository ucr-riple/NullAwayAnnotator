/*
 * Copyright (c) 2025 University of California, Riverside.
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

package edu.ucr.cs.riple.injector.changes;

import edu.ucr.cs.riple.injector.location.OnMethod;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** A change that rewrites a method declaration in the source code. */
public class MethodRewriteChange extends RegionRewrite {

  public MethodRewriteChange(OnMethod location, String newMethod) {
    this(location, newMethod, new HashSet<>());
  }

  public MethodRewriteChange(OnMethod location, String newMethod, Set<String> imports) {
    super(location, newMethod, imports);
  }

  @Override
  public ASTChange copy() {
    return new MethodRewriteChange((OnMethod) location, newRegion, imports);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof MethodRewriteChange)) {
      return false;
    }
    MethodRewriteChange that = (MethodRewriteChange) o;
    return Objects.equals(getLocation(), that.getLocation())
        && Objects.equals(newRegion, that.newRegion);
  }
}
