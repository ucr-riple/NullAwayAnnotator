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
import java.util.Collection;
import javax.annotation.Nullable;

public interface ImpactCache<T extends Impact> {

  boolean isUnknown(Fix fix);

  /**
   * Collects set of triggered errors if given fix is applied.
   *
   * @param fix Fix instance to be applied in the target module.
   * @return Set of triggered errors.
   */
  ImmutableSet<Error> getTriggeredErrors(Fix fix);

  /**
   * Updates store after injection of fixes in target module.
   *
   * @param fixes Set of injected fixes.
   */
  void updateImpactsAfterInjection(Collection<Fix> fixes);

  /**
   * Retrieves the corresponding {@link Impact} of a fix.
   *
   * @param fix Target fix.
   * @return Corresponding {@link Impact}, null if not located.
   */
  @Nullable
  T fetchImpact(Fix fix);

  /**
   * Returns Set of errors that will be triggered if given fixes are applied.
   *
   * @param fixes Collection of given fixes.
   * @return Immutable set of triggered fixes.
   */
  ImmutableSet<Error> getTriggeredErrorsForCollection(Collection<Fix> fixes);
}
