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

package edu.ucr.cs.riple.core.cache.downstream;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.evaluators.BasicEvaluator;
import edu.ucr.cs.riple.core.evaluators.suppliers.DownstreamDependencySupplier;
import edu.ucr.cs.riple.injector.location.Location;

/**
 * Evaluator for analyzing downstream dependencies. Used by {@link DownstreamImpactCacheImpl} to
 * compute the effects of changes in upstream on downstream dependencies. This evaluator cannot be
 * used to compute the effects in target module.
 */
class DownstreamImpactEvaluator extends BasicEvaluator {

  public DownstreamImpactEvaluator(DownstreamDependencySupplier supplier) {
    super(supplier);
  }

  @Override
  protected void collectGraphResults(ImmutableSet<Report> reports) {
    super.collectGraphResults(reports);
    // Update path for each location instances if declared in target module in the computed
    // triggered fixes. These triggered fixes do not have an actual physical path since they are
    // provided as a jar file in downstream dependencies.
    this.graph
        .getNodes()
        .forEach(
            node -> {
              node.triggeredErrors.forEach(
                  error ->
                      error
                          .getResolvingFixes()
                          .forEach(
                              fix ->
                                  fix.changes.forEach(
                                      annot -> {
                                        Location location = annot.getLocation();
                                        // check if the location is inside the target module.
                                        if (context.targetModuleInfo.declaredInModule(location)) {
                                          location.path =
                                              context.targetModuleInfo.getLocationOnClass(
                                                      location.clazz)
                                                  .path;
                                        }
                                      })));
            });
  }
}
