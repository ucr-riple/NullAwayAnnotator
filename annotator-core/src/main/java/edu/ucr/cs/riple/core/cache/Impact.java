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

package edu.ucr.cs.riple.core.cache;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.injector.location.Location;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/** Stores the set of errors which will be triggered if the containing fix is applied. */
public class Impact {

  /** Target fix. */
  public final Fix fix;
  /** List of triggered errors, if this fix is applied to source code. */
  protected ImmutableSet<Error> triggeredErrors;

  public Impact(Fix fix) {
    this.fix = fix;
    this.triggeredErrors = ImmutableSet.of();
  }

  public Impact(Fix fix, Set<Error> triggeredErrors) {
    this.fix = fix;
    this.triggeredErrors = ImmutableSet.copyOf(triggeredErrors);
  }

  /**
   * Updates state after injection of the given fixes permanently by removing triggered errors that
   * are resolved.
   *
   * @param fixes Set of applied fixes to source code permanently.
   */
  public void updateStatusAfterInjection(Collection<Fix> fixes) {
    triggeredErrors =
        ImmutableSet.copyOf(
            triggeredErrors.stream()
                .filter(error -> !error.isResolvableWith(fixes))
                .collect(Collectors.toSet()));
  }

  /**
   * Returns list of triggered errors if method is {@code @Nullable} on downstream dependencies.
   *
   * @return Set of errors.
   */
  public ImmutableSet<Error> getTriggeredErrors() {
    return triggeredErrors;
  }

  /**
   * Gets the containing location.
   *
   * @return Containing fix location.
   */
  public Location toLocation() {
    return fix.toLocation();
  }
}
