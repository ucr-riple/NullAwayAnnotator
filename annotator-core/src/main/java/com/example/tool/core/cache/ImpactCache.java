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

import java.util.Collection;
import javax.annotation.Nullable;

/**
 * Interface for a cache saving impacts of fixes on modules. An impact of a fix, is a collection of
 * triggered errors and fixes when the fix is applied.
 *
 * @param <T> Type of impact stored in cache.
 */
public interface ImpactCache<T extends Impact> {

  /**
   * Checks if the impacts of the given fix are stored.
   *
   * @param fix Given fix.
   * @return True, if impacts are stored.
   */
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
   * Returns Set of errors that will be triggered if given fixes are applied to target module.
   *
   * @param fixes Collection of given fixes.
   * @return Immutable set of triggered errors.
   */
  ImmutableSet<Error> getTriggeredErrorsForCollection(Collection<Fix> fixes);

  /**
   * Returns Set of fixes on downstream dependencies that will be triggered if given fixes are
   * applied to target module.
   *
   * @param fixes Collection of given fixes.
   * @return Immutable set of triggered fixes.
   */
  ImmutableSet<Fix> getTriggeredFixesOnDownstreamForCollection(Collection<Fix> fixes);

  /**
   * Returns number of stored entries in cache store.
   *
   * @return Number of entries.
   */
  int size();
}
