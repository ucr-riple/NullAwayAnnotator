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

package edu.ucr.cs.riple.annotatorcore.explorers.suppliers;

import edu.ucr.cs.riple.annotatorcore.Config;
import edu.ucr.cs.riple.annotatorcore.injectors.AnnotationInjector;
import edu.ucr.cs.riple.annotatorcore.injectors.VirtualInjector;
import edu.ucr.cs.riple.annotatorcore.metadata.method.MethodDeclarationTree;

/**
 * Supplier for downstream dependency analysis. It has the following characteristics:
 *
 * <ul>
 *   <li>Annotations are virtually injected on downstream dependencies.
 *   <li>Analysis is performed only to depth 1.
 *   <li>Global impact of annotations are neglected.
 * </ul>
 */
public class DownstreamDependencySupplier extends AbstractSupplier {

  public DownstreamDependencySupplier(Config config, MethodDeclarationTree tree) {
    super(config.downstreamInfo, config, tree);
  }

  @Override
  protected AnnotationInjector initializeInjector() {
    return new VirtualInjector(config);
  }

  @Override
  protected int initializeDepth() {
    return 1;
  }
}