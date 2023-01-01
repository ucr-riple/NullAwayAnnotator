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

import edu.ucr.cs.riple.core.evaluators.BasicEvaluator;
import edu.ucr.cs.riple.core.evaluators.suppliers.DownstreamDependencySupplier;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.method.MethodDeclarationTree;
import edu.ucr.cs.riple.injector.location.OnMethod;
import edu.ucr.cs.riple.injector.location.OnParameter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Evaluator for analyzing downstream dependencies. Used by {@link GlobalAnalyzerImpl} to compute
 * the effects of changes in upstream on downstream dependencies. This evaluator cannot be used to
 * compute the effects in target module.
 */
class DownstreamImpactEvaluator extends BasicEvaluator {

  /**
   * Map of public methods in target module to parameters in target module, which are source of
   * nullable flow back to upstream module (target) from downstream dependencies, if annotated as
   * {@code @Nullable}.
   */
  private final HashMap<OnMethod, Set<OnParameter>> nullableFlowMap;

  private final MethodDeclarationTree methodDeclarationTree;

  public DownstreamImpactEvaluator(DownstreamDependencySupplier supplier) {
    super(supplier);
    this.nullableFlowMap = new HashMap<>();
    this.methodDeclarationTree = supplier.getMethodDeclarationTree();
  }

  @Override
  protected void collectGraphResults() {
    super.collectGraphResults();
    // Collect impacted parameters in target module by downstream dependencies.
    this.graph
        .getNodes()
        .forEach(
            node ->
                node.root.ifOnMethod(
                    method -> {
                      // Impacted parameters.
                      Set<OnParameter> parameters =
                          node.triggeredErrors.stream()
                              .filter(
                                  error ->
                                      error.isSingleFix()
                                          && error.toResolvingLocation().isOnParameter()
                                          // Method is declared in the target module.
                                          && methodDeclarationTree.declaredInModule(
                                              error.toResolvingParameter()))
                              .map(Error::toResolvingParameter)
                              .collect(Collectors.toSet());
                      if (!parameters.isEmpty()) {
                        // Update uri for each parameter. These triggered fixes does not have an
                        // actual physical uri since they are provided as a jar file in downstream
                        // dependencies.
                        parameters.forEach(
                            onParameter ->
                                onParameter.path =
                                    methodDeclarationTree.findNode(
                                            onParameter.method, onParameter.clazz)
                                        .location
                                        .path);
                        nullableFlowMap.put(method, parameters);
                      }
                    }));
  }

  /**
   * Returns set of parameters that will receive {@code @Nullable} if the passed method is annotated
   * as {@code @Nullable}.
   *
   * @param method Method to be annotated.
   * @return Set of impacted parameters. If no parameter is impacted, empty set will be returned.
   */
  public Set<OnParameter> getImpactedParameters(OnMethod method) {
    return nullableFlowMap.getOrDefault(method, Collections.emptySet());
  }
}
