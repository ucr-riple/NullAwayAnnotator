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

package edu.ucr.cs.riple.core.global;

import edu.ucr.cs.riple.core.metadata.method.MethodDeclarationTree;
import edu.ucr.cs.riple.core.metadata.method.MethodNode;
import edu.ucr.cs.riple.injector.location.OnParameter;
import java.util.Set;

/** Container class for storing overall effect of each method. */
public class MethodImpact {
  /** Node in {@link MethodDeclarationTree} corresponding to a public method. */
  final MethodNode node;
  /**
   * Set of parameters in target module that will receive {@code Nullable} value if targeted method
   * in node is annotated as {@code @Nullable}.
   */
  public Set<OnParameter> impactedParameters;
  /**
   * Effect of injecting a {@code Nullable} annotation on pointing method of node on downstream
   * dependencies.
   */
  int effect;

  public MethodImpact(MethodNode node) {
    this.node = node;
    this.effect = 0;
  }

  @Override
  public int hashCode() {
    return hash(node.location.method, node.location.clazz);
  }

  /**
   * Calculates hash. This method is used outside this class to calculate the expected hash based on
   * instance's properties value if the actual instance is not available.
   *
   * @param method Method signature.
   * @param clazz Fully qualified name of the containing class.
   * @return Expected hash.
   */
  public static int hash(String method, String clazz) {
    return MethodNode.hash(method, clazz);
  }
}
