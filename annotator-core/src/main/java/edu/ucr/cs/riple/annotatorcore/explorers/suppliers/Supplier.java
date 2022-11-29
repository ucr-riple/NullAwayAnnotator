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
import edu.ucr.cs.riple.annotatorcore.metadata.index.Bank;
import edu.ucr.cs.riple.annotatorcore.metadata.index.Error;
import edu.ucr.cs.riple.annotatorcore.metadata.index.Fix;
import edu.ucr.cs.riple.annotatorcore.metadata.method.MethodDeclarationTree;

/**
 * Supplier for initializing an {@link edu.ucr.cs.riple.annotatorcore.explorers.Explorer} instance.
 */
public interface Supplier {

  /**
   * Getter for {@link Bank} of {@link Fix} instances.
   *
   * @return Fix Bank instance.
   */
  Bank<Fix> getFixBank();

  /**
   * Getter for {@link Bank} of {@link Error} instance.
   *
   * @return Error Bank instance.
   */
  Bank<Error> getErrorBank();

  /**
   * Getter for {@link AnnotationInjector} instance.
   *
   * @return Annotation Injector instance.
   */
  AnnotationInjector getInjector();

  /**
   * Getter for {@link MethodDeclarationTree} instance.
   *
   * @return MethodDeclarationTree instance.
   */
  MethodDeclarationTree getMethodDeclarationTree();

  /**
   * Getter for depth of analysis.
   *
   * @return depth.
   */
  int depth();

  /**
   * Getter for {@link Config} instance.
   *
   * @return Config instance.
   */
  Config getConfig();
}
