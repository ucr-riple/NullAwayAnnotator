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
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.explorers.OptimizedExplorer;
import edu.ucr.cs.riple.core.injectors.AnnotationInjector;
import edu.ucr.cs.riple.core.metadata.index.Bank;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.method.MethodDeclarationTree;
import edu.ucr.cs.riple.core.metadata.trackers.RegionTracker;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.location.OnMethod;
import edu.ucr.cs.riple.injector.location.OnParameter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

/** Explorer for analyzing downstream dependencies. */
public class DownstreamImpactAnalyzer extends OptimizedExplorer {

  /**
   * Map of public methods in target module to parameters in target module, which are source of
   * nullable flow back to upstream module (target) from downstream dependencies, if annotated as
   * {@code @Nullable}.
   */
  private final HashMap<OnMethod, Set<OnParameter>> nullableFlowMap;

  public DownstreamImpactAnalyzer(
      AnnotationInjector injector,
      Bank<Error> errorBank,
      Bank<Fix> fixBank,
      RegionTracker tracker,
      ImmutableSet<Fix> fixes,
      MethodDeclarationTree methodDeclarationTree,
      int depth,
      Config config) {
    super(injector, errorBank, fixBank, tracker, fixes, methodDeclarationTree, depth, config);
    this.nullableFlowMap = new HashMap<>();
  }

  @Override
  public void rerunAnalysis() {
    Utility.buildDownstreamDependencies(config);
  }

  @Override
  protected void finalizeReports() {
    super.finalizeReports();
    // Collect impacted parameters in target module by downstream dependencies.
    graph
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
                                      error.nonnullTarget != null
                                          && error.nonnullTarget.isOnParameter()
                                          // Method is declared in the target module.
                                          && methodDeclarationTree.declaredInModule(
                                              error.nonnullTarget.toMethod()))
                              .map(error -> error.nonnullTarget.toParameter())
                              .collect(Collectors.toSet());
                      if (!parameters.isEmpty()) {
                        // Update uri for each parameter. These triggered fixes does not have an
                        // actual physical uri since they are provided as a jar file in downstream
                        // dependencies.
                        parameters.forEach(
                            onParameter ->
                                onParameter.uri =
                                    methodDeclarationTree.findNode(
                                            onParameter.method, onParameter.clazz)
                                        .location
                                        .uri);
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
