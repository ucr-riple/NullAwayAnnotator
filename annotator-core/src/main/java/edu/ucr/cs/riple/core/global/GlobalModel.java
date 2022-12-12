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

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.model.Model;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Set;

/**
 * Model for downstream dependencies.
 *
 * <p>This class stores impacts of switching the nullability of public APIs of one compilation
 * target, in order to compute the effect on said target's downstream dependencies of adding
 * annotations to those APIs. It does so by building said downstream dependencies. It collects the
 * effect (number of additional errors) of each such change, by summing across all downstream
 * dependencies. This data then be fed to the Annotator main process in the decision process.
 */
public interface GlobalModel extends Model<MethodImpact> {

  /** Analyzes effects of changes in public methods in downstream dependencies. */
  void analyzeDownstreamDependencies();

  /**
   * Returns the lower bound of number of errors of applying a fix and its associated chain of fixes
   * on the target on downstream dependencies.
   *
   * @param tree Tree of the fix tree associated to root.
   * @return Lower bound of number of errors on downstream dependencies.
   */
  int computeLowerBoundOfNumberOfErrors(Set<Fix> tree);

  /**
   * Returns the upper bound of number of errors of applying a fix and its associated chain of fixes
   * on the target on downstream dependencies.
   *
   * @param tree Tree of the fix tree associated to root.
   * @return Upper bound of number of errors on downstream dependencies.
   */
  int computeUpperBoundOfNumberOfErrors(Set<Fix> tree);

  /**
   * Updates state of methods after injection of fixes in target module.
   *
   * @param fixes Set of injected fixes.
   */
  void updateImpactsAfterInjection(Set<Fix> fixes);
}
