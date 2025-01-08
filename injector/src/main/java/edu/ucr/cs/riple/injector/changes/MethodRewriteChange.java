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

import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.NodeWithRange;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.OnMethod;
import edu.ucr.cs.riple.injector.modifications.Replacement;
import java.util.Objects;
import javax.annotation.Nullable;

public class MethodRewriteChange implements ASTChange {

  /** Location of the method to be rewritten. */
  private final OnMethod location;

  /** New method to replace the old method. This should be a valid Java method declaration. */
  private final String newMethod;

  public MethodRewriteChange(OnMethod location, String newMethod) {
    this.location = location;
    this.newMethod = newMethod;
  }

  @Nullable
  @Override
  public <T extends NodeWithAnnotations<?> & NodeWithRange<?>>
      Replacement computeTextModificationOn(T node) {
    if (node.getRange().isEmpty()) {
      return null;
    }
    return new Replacement(newMethod, node.getRange().get().begin, node.getRange().get().end);
  }

  @Override
  public Location getLocation() {
    return location;
  }

  @Override
  public ASTChange copy() {
    return new MethodRewriteChange(location, newMethod);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof MethodRewriteChange)) {
      return false;
    }
    MethodRewriteChange that = (MethodRewriteChange) o;
    return Objects.equals(getLocation(), that.getLocation())
        && Objects.equals(newMethod, that.newMethod);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getLocation(), newMethod);
  }
}
