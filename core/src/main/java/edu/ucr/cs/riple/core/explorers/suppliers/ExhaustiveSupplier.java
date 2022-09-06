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

package edu.ucr.cs.riple.core.explorers.suppliers;

import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.global.GlobalAnalyzer;
import edu.ucr.cs.riple.core.global.NoOpGlobalAnalyzer;
import edu.ucr.cs.riple.core.metadata.method.MethodDeclarationTree;

/** Supplier for exhaustive analysis. This will be mostly used in experiments. */
public class ExhaustiveSupplier extends TargetModuleSupplier {

  /**
   * Constructor for exhaustive supplier.
   *
   * @param config Annotator config instance.
   * @param tree Method declaration tree instance for methods in target module.
   */
  public ExhaustiveSupplier(Config config, MethodDeclarationTree tree) {
    super(config, tree);
  }

  @Override
  public GlobalAnalyzer getGlobalAnalyzer() {
    // Exhaustive search does not consider the effect of fixes, it applies all suggested fixes.
    return new NoOpGlobalAnalyzer();
  }
}
