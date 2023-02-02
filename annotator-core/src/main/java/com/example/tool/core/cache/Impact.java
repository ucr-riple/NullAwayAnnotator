/*
 * MIT License
 *
 * Copyright (c) 2022 anonymous
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

package com.example.tool.core.cache;

import com.example.tool.core.metadata.index.Error;
import com.example.tool.core.metadata.index.Fix;
import com.google.common.collect.ImmutableSet;
import com.example.tool.injector.location.Location;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/** Stores the set of errors which will be triggered if the containing fix is applied. */
public class Impact {

  /** Target fix. */
  public final Fix fix;
  /** Set of triggered errors, if this fix is applied to source code. */
  protected ImmutableSet<Error> triggeredErrors;
  /** Set of triggered fixes on downstream dependencies if containing fix is applied. */
  protected ImmutableSet<Fix> triggeredFixesOnDownstream;

  public Impact(Fix fix) {
    this.fix = fix;
    this.triggeredErrors = ImmutableSet.of();
    this.triggeredFixesOnDownstream = ImmutableSet.of();
  }

  public Impact(Fix fix, Set<Error> triggeredErrors, Set<Fix> triggeredFixesOnDownstream) {
    this.fix = fix;
    this.triggeredErrors = ImmutableSet.copyOf(triggeredErrors);
    this.triggeredFixesOnDownstream = ImmutableSet.copyOf(triggeredFixesOnDownstream);
  }

  /**
   * Updates state after injection of the given fixes permanently by removing triggered errors that
   * are resolved.
   *
   * @param fixes Set of applied fixes to source code permanently.
   */
  public void updateStatusAfterInjection(Collection<Fix> fixes) {
    triggeredErrors =
        triggeredErrors.stream()
            .filter(error -> !error.isResolvableWith(fixes))
            .collect(ImmutableSet.toImmutableSet());
  }

  /**
   * Returns list of triggered errors.
   *
   * @return Immutable Set of errors.
   */
  public ImmutableSet<Error> getTriggeredErrors() {
    return triggeredErrors;
  }

  /**
   * Returns list of triggered fixes on downstream dependencies if this fix is applied.
   *
   * @return Immutable Set of fixes.
   */
  public ImmutableSet<Fix> getTriggeredFixesOnDownstream() {
    return triggeredFixesOnDownstream;
  }

  /**
   * Gets the containing location.
   *
   * @return Containing fix location.
   */
  public Location toLocation() {
    return fix.toLocation();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Impact)) {
      return false;
    }
    Impact impact = (Impact) o;
    return fix.equals(impact.fix);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fix);
  }
}
