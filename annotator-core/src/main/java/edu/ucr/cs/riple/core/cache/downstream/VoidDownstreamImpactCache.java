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
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import java.util.Collection;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * This downstream impact cache does not have any information regarding the impact of changes in
 * target module in dependencies, the main purpose of this class is to avoid initializing downstream
 * impact cache instances to {@code null} when impact on dependencies is not considered.
 */
public class VoidDownstreamImpactCache implements DownstreamImpactCache {

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
  public boolean triggersUnresolvableErrorsOnDownstream(Fix fix) {
    return false;
  }

  @Override
  public boolean isUnknown(Fix fix) {
    return true;
  }

  @Override
  public ImmutableSet<Error> getTriggeredErrors(Fix fix) {
    return ImmutableSet.of();
  }

  @Override
  public void updateImpactsAfterInjection(Collection<Fix> fixes) {}

  @Nullable
  @Override
  public DownstreamImpact fetchImpact(Fix fix) {
    return null;
  }

  @Override
  public ImmutableSet<Error> getTriggeredErrorsForCollection(Collection<Fix> fixes) {
    return ImmutableSet.of();
  }

  @Override
  public ImmutableSet<Fix> getTriggeredFixesOnDownstreamForCollection(Collection<Fix> fixes) {
    return ImmutableSet.of();
  }

  @Override
  public int size() {
    return 0;
  }
}
