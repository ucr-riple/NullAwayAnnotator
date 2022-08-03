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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.metadata.trackers.Region;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Responsible for tracking status of generated outputs. It indexes outputs, can save states and
 * compute the difference between states.
 *
 * @param <T> Extends Enclosed.
 */
public class Bank<T extends Enclosed> {

  /** Initial state indexed by enclosing class. */
  private final Index<T> rootInClass;
  /** Initial state indexed by enclosing class and method. */
  private final Index<T> rootInMethod;
  /** Current state indexed by enclosing class. */
  private Index<T> currentInClass;
  /** Current state indexed by enclosing class and method. */
  private Index<T> currentInMethod;
  /** Factory instance to parse outputs. */
  private final Factory<T> factory;
  /** Path to file where outputs are stored. */
  private final ImmutableSet<Path> paths;

  public Bank(ImmutableSet<Path> paths, Factory<T> factory) {
    this.factory = factory;
    this.paths = paths;
    rootInClass = new Index<>(this.paths, Index.Type.BY_CLASS, factory);
    rootInMethod = new Index<>(this.paths, Index.Type.BY_METHOD, factory);
    rootInMethod.index();
    rootInClass.index();
    Preconditions.checkArgument(rootInClass.total == rootInMethod.total);
  }

  public Bank(Path path, Factory<T> factory) {
    this(ImmutableSet.of(path), factory);
  }

  /**
   * Overwrites the current state with the new generated output,
   *
   * @param saveClass If true, current index by class will be updated.
   * @param saveMethod if true, current index by class and method will be updated.
   */
  public void saveState(boolean saveClass, boolean saveMethod) {
    if (saveClass) {
      currentInClass = new Index<>(paths, Index.Type.BY_CLASS, factory);
      currentInClass.index();
    }
    if (saveMethod) {
      currentInMethod = new Index<>(paths, Index.Type.BY_METHOD, factory);
      currentInMethod.index();
    }
  }

  /**
   * Computes the difference between two collections (A - B).
   *
   * @param previousItems B.
   * @param currentItems A.
   * @return Corresponding {@link Result} instance storing result of (A - B).
   */
  private Result<T> compareByList(Collection<T> previousItems, Collection<T> currentItems) {
    int size = currentItems.size() - previousItems.size();
    previousItems.forEach(currentItems::remove);
    return new Result<>(size, currentItems);
  }

  /**
   * Computes the difference in items enclosed by the given enclosing class and method in current
   * state and root state.
   *
   * @param encClass Enclosing fully qualified class name.
   * @param encMethod Enclosing method signature.
   * @return Corresponding {@link Result}.
   */
  public Result<T> compareByMethod(String encClass, String encMethod, boolean fresh) {
    saveState(false, fresh);
    return compareByList(
        rootInMethod.getByMethod(encClass, encMethod),
        currentInMethod.getByMethod(encClass, encMethod));
  }

  /**
   * Computes the difference in current state and root state.
   *
   * @return Corresponding {@link Result} instance.
   */
  public Result<T> compare() {
    return compareByList(rootInMethod.values(), currentInMethod.values());
  }

  /**
   * Returns all items regions that holds the given predicate.
   *
   * @param predicate Predicate provided by caller.
   * @return Set of regions.
   */
  public Set<Region> getRegionsForFixes(Predicate<T> predicate) {
    return rootInClass.getRegionsOfMatchingItems(predicate);
  }
}
