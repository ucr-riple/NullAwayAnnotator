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
import edu.ucr.cs.riple.core.metadata.method.MethodNode;
import edu.ucr.cs.riple.core.model.Impact;
import edu.ucr.cs.riple.injector.location.OnMethod;
import edu.ucr.cs.riple.injector.location.OnParameter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Container class for storing overall effect of each method in downstream dependencies. */
public class MethodImpact extends Impact {

  /**
   * Map of parameters in target module that will receive {@code Nullable} value if targeted method
   * in node is annotated as {@code @Nullable} with their corresponding triggered errors.
   */
  private final HashMap<OnParameter, List<Error>> impactedParametersMap;

  public MethodImpact(Fix fix) {
    super(fix);
    this.impactedParametersMap = new HashMap<>();
    this.triggeredErrors = new HashSet<>();
  }

  @Override
  public int hashCode() {
    return hash(fix.toMethod().method, fix.toMethod().clazz);
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
    this.triggeredErrors = new HashSet<>(report.triggeredErrors);
    // Count the number of times each parameter received a @Nullable.
    impactedParameters.forEach(
        onParameter -> {
          List<Error> triggered =
              triggeredErrors.stream()
                  .filter(
                      error ->
                          error.isSingleFix() && error.toResolvingLocation().equals(onParameter))
                  .collect(Collectors.toList());
          impactedParametersMap.put(onParameter, triggered);
        });
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
  @Override
  public void updateStatusAfterInjection(Collection<Fix> fixes) {
    Set<OnParameter> annotatedParameters = new HashSet<>();
    fixes.forEach(
        fix ->
            fix.ifOnParameter(
                onParameter -> {
                  if (impactedParametersMap.containsKey(onParameter)) {
                    List<Error> errors = impactedParametersMap.get(onParameter);
                    triggeredErrors.removeAll(errors);
                    annotatedParameters.add(onParameter);
                  }
                }));
    annotatedParameters.forEach(impactedParametersMap::remove);
  }

  /**
   * Gets the containing method location.
   *
   * @return Containing method location.
   */
  public OnMethod toMethod() {
    return fix.toMethod();
  }
}
