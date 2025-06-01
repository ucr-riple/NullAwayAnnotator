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
import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.modifications.Replacement;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;

public abstract class RegionRewrite implements ASTChange {

  /** Location of the region to be rewritten. */
  protected final Location location;

  /** New region content to replace the old region. */
  protected final String newRegion;

  /** Set of imports that should be added to the file with the new declaration. */
  protected final Set<String> imports;

  public RegionRewrite(Location location, String newRegion) {
    this(location, newRegion, new HashSet<>());
  }

  public RegionRewrite(Location location, String newRegion, Set<String> imports) {
    this.location = location;
    this.newRegion = newRegion;
    this.imports = imports;
  }

  @Nullable
  @Override
  public <T extends NodeWithAnnotations<?> & NodeWithRange<?>>
      Replacement computeTextModificationOn(T node) {
    if (node.getRange().isEmpty()) {
      return null;
    }
    // find the indent of the method's first line
    int indent = node.getRange().get().begin.column - 1;
    // indent the new method
    String indentedMethod = newRegion.replaceAll("\n", "\n" + " ".repeat(indent));
    return new Replacement(indentedMethod, node.getRange().get().begin, node.getRange().get().end);
  }

  /**
   * Add an import statement to the set of imports that should be added to the file with the new
   * method.
   *
   * @param importStatement the import statement to add
   */
  public void addImport(String importStatement) {
    imports.add(importStatement);
  }

  /**
   * Get the set of imports that should be added to the file with the new method.
   *
   * @return the set of imports
   */
  public ImmutableSet<String> getImports() {
    return ImmutableSet.copyOf(imports);
  }

  @Override
  public Location getLocation() {
    return location;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getLocation(), newRegion);
  }

  @Override
  public String toString() {
    return "location=" + location + ", content='" + newRegion + '\'' + '}';
  }
}
