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

package edu.ucr.cs.riple.core;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import java.util.HashMap;
import java.util.Map;

/** Reports cache. Used to detect fixes that has already been processed. */
public class ReportCache {

  /**
   * Map of fix to their corresponding reports, used to detect processed fixes in a new batch of fix
   * suggestion. Note: Set does not have get method, here we use map which retrieves elements
   * efficiently.
   */
  private final Map<Fix, Report> store;
  /** Cache activation switch. */
  private boolean enabled;

  /**
   * If true, the content of cache has been updated when {@link ReportCache#update(ImmutableSet)}
   * method is called. Used to determine if annotator should perform another iteration of analysis.
   */
  private boolean stateUpdated;

  public ReportCache(CLI cli) {
    this.store = new HashMap<>();
    this.enabled = cli.useCache;
    this.stateUpdated = true;
  }

  /**
   * Checks if the corresponding report is stored.
   *
   * @param fix Fix instance.
   * @return true if the fix is already processed.
   */
  public boolean processedFix(Fix fix) {
    if (!enabled) {
      return false;
    }
    return store.containsKey(fix);
  }

  /**
   * Updates the state of cache with the latest processed reports.
   *
   * @param reports Set of the latest processed reports.
   */
  public void update(ImmutableSet<Report> reports) {
    int size = store.keySet().size();
    reports.forEach(report -> store.put(report.root, report));
    if (size == store.keySet().size()) {
      stateUpdated = false;
    }
  }

  /**
   * Returns true if the content of cache is updated after calling update method.
   *
   * @return true if the content has been updated.
   */
  public boolean isUpdated() {
    return stateUpdated;
  }

  /** Enables cache. */
  public void enable() {
    this.enabled = true;
  }

  /** Disabled cache. */
  public void disable() {
    this.enabled = false;
  }

  /**
   * Getter for all stored reports.
   *
   * @return Immutable set of stored reports.
   */
  public ImmutableSet<Report> reports() {
    return ImmutableSet.copyOf(store.values());
  }
}
