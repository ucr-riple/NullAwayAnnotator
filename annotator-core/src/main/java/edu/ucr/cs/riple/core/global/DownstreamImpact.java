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

import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.method.MethodDeclarationTree;
import edu.ucr.cs.riple.core.metadata.method.MethodNode;
import edu.ucr.cs.riple.injector.location.OnParameter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Container class for storing overall effect of each method. */
public class DownstreamImpact {
  /** Node in {@link MethodDeclarationTree} corresponding to a public method. */
  final MethodNode node;
  /**
   * Map of parameters in target module that will receive {@code Nullable} value if targeted method
   * in node is annotated as {@code @Nullable} with their corresponding triggered errors.
   */
  private final HashMap<OnParameter, Set<Error>> impactedParametersMap;
  /**
   * Set of triggered errors in downstream dependencies if target method in node is annotated as
   * {@code @Nullable}.
   */
  private Set<Error> triggeredErrors;
  /**
   * Effect of injecting a {@code Nullable} annotation on pointing method of node on downstream
   * dependencies.
   */
  private int effect;

  public DownstreamImpact(MethodNode node) {
    this.node = node;
    this.effect = 0;
    this.impactedParametersMap = new HashMap<>();
    this.triggeredErrors = new HashSet<>();
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

  /**
   * Updates the status of methods impact on downstream dependencies.
   *
   * @param report Result of applying making method in node {@code @Nullable} in downstream
   *     dependencies.
   * @param impactedParameters Set of impacted paramaters.
   */
  public void setStatus(Report report, Set<OnParameter> impactedParameters) {
    this.effect = report.localEffect;
    this.triggeredErrors = new HashSet<>(report.triggeredErrors);
    // Count the number of times each parameter received a @Nullable.
    impactedParameters.forEach(
        onParameter -> {
          Set<Error> triggered =
              triggeredErrors.stream()
                  .filter(
                      error ->
                          error.isSingleFix() && error.toResolvingLocation().equals(onParameter))
                  .collect(Collectors.toSet());
          impactedParametersMap.put(onParameter, triggered);
        });
  }

  /**
   * Getter for effect.
   *
   * @return Effect.
   */
  public int getEffect() {
    return effect;
  }

  /**
   * Returns set of triggered errors if method is {@code @Nullable} on downstream dependencies.
   *
   * @return Set of errors.
   */
  public Set<Error> getTriggeredErrors() {
    return triggeredErrors;
  }

  /**
   * Returns set of parameters on target module that will receive {@code @Nullable} if method in
   * node is annotated as {@code @Nullable}.
   *
   * @return Set of parameters location.
   */
  public Set<OnParameter> getImpactedParameters() {
    return impactedParametersMap.keySet();
  }

  /**
   * Updates the status of method's impact after injection of fixes in target module. Potentially
   * part of stored impact result is invalid due to injection of fixes. (e.g. some impacted
   * parameters may already be annotated as {@code @Nullable} and will no longer trigger errors on
   * downstream dependencies). This method addresses this issue by updating method's status.
   *
   * @param fixes List of injected fixes.
   */
  public void updateStatus(Set<Fix> fixes) {
    Set<OnParameter> annotatedParameters = new HashSet<>();
    fixes.forEach(
        fix ->
            fix.ifOnParameter(
                onParameter -> {
                  if (impactedParametersMap.containsKey(onParameter)) {
                    Set<Error> errors = impactedParametersMap.get(onParameter);
                    effect -= errors.size();
                    triggeredErrors.removeAll(errors);
                    annotatedParameters.add(onParameter);
                  }
                }));
    if (effect < 0) {
      // This is impossible, however for safety issues, we set it to zero.
      effect = 0;
    }
    annotatedParameters.forEach(impactedParametersMap::remove);
  }
}
