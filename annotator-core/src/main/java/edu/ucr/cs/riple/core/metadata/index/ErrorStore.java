/*
 * MIT License
 *
 * Copyright (c) 2020 Nima Karimipour
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

package edu.ucr.cs.riple.core.metadata.index;

import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.Context;
import edu.ucr.cs.riple.core.metadata.trackers.Region;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Responsible for tracking status of generated outputs. It indexes outputs, can save states and
 * compute the difference between states.
 */
public class ErrorStore {

  /** Initial state indexed by enclosing class and method. */
  private final Index root;
  /** Current state indexed by enclosing class and method. */
  private Index current;

  private final Context context;
  private final Config config;

  public ErrorStore(Config config, Context context) {
    this.context = context;
    this.config = config;
    root = new Index(config, context);
    root.index();
  }

  /** Overwrites the current state with the new generated output, */
  public void saveState() {
    current = new Index(config, context);
    current.index();
  }

  /**
   * Computes the difference between two collections (A - B).
   *
   * @param previousItems B.
   * @param currentItems A.
   * @return Corresponding {@link Result} instance storing result of (A - B).
   */
  private Result compareByList(Collection<Error> previousItems, Collection<Error> currentItems) {
    int size = currentItems.size() - previousItems.size();
    List<Error> temp = new ArrayList<>(currentItems);
    previousItems.forEach(temp::remove);
    return new Result(size, temp);
  }

  /**
   * Computes the difference in items enclosed by the given enclosing class and member in current
   * state and root state.
   *
   * @param region Enclosing region
   * @return Corresponding {@link Result}.
   */
  public Result compareByRegion(Region region) {
    return compareByList(root.get(region), current.get(region));
  }

  /**
   * Computes the difference in current state and root state.
   *
   * @return Corresponding {@link Result} instance.
   */
  public Result compare() {
    return compareByList(root.values(), current.values());
  }

  /**
   * Returns all items regions that holds the given predicate.
   *
   * @param predicate Predicate provided by caller.
   * @return Set of regions.
   */
  public Set<Region> getRegionsForElements(Predicate<Error> predicate) {
    return root.getRegionsOfMatchingItems(predicate);
  }

  /**
   * Returns the number of resolved errors from base (initial set of errors) by the given fixes.
   *
   * @param fixes The given fixes.
   * @return Number of resolved errors.
   */
  public int getNumberOfErrorsResolvedByAllFixesWithinCollection(Collection<Fix> fixes) {
    return (int) root.values().stream().filter(error -> error.isResolvableWith(fixes)).count();
  }
}
