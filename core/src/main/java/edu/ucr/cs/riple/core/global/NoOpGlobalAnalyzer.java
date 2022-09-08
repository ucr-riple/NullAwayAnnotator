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
import edu.ucr.cs.riple.injector.location.OnParameter;
import java.util.List;
import java.util.Set;

/**
 * This global analyzer does not have any information regarding the impact of changes in target
 * module in dependencies, the main purpose of this class is to avoid initializing GlobalAnalyzer
 * instances to {@code null} when impact on dependencies is not considered.
 */
public class NoOpGlobalAnalyzer implements GlobalAnalyzer {

  @Override
  public void analyzeDownstreamDependencies() {
    // No operation needed.
  }

  @Override
  public int computeLowerBoundOfNumberOfErrors(Set<Fix> tree) {
    return 0;
  }

  @Override
  public int computeUpperBoundOfNumberOfErrors(Set<Fix> tree) {
    return 0;
  }

  @Override
  public ImmutableSet<OnParameter> getImpactedParameters(Set<Fix> fixTree) {
    return ImmutableSet.of();
  }

  @Override
  public List<Error> getTriggeredErrors(Fix fix) {
    return List.of();
  }

  @Override
  public void updateImpactsAfterInjection(Set<Fix> fixes) {
    // No operation needed.
  }

  @Override
  public boolean isFixForcingDownstreamChanges(Fix fix) {
    return false;
  }
}
