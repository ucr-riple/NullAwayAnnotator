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

package edu.ucr.cs.riple.core.log;

import edu.ucr.cs.riple.core.evaluators.graph.ConflictGraph;
import edu.ucr.cs.riple.injector.changes.AddAnnotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Log information for Annotator. */
public class Log {

  /** Sum of number of nodes constructed in each {@link ConflictGraph}. */
  private long nodes;

  /** Number of build requests. */
  private long requested;

  /** Total time spent for annotator from start to finish. */
  private long totalTime;

  /** Total time spent in building targets. */
  private long buildTime = 0;

  /**
   * Set of approved and injected annotations. These annotations are evaluated and approved and will
   * not get removed from the source code.
   */
  private final List<AddAnnotation> injectedAnnotations = new ArrayList<>();

  public Log() {
    this.reset();
  }

  /** Resets all log information. */
  public void reset() {
    this.nodes = 0;
    this.requested = 0;
    this.totalTime = 0;
    this.buildTime = 0;
    this.injectedAnnotations.clear();
  }

  @Override
  public String toString() {
    return "Total number of nodes="
        + nodes
        + "\nTotal number of Requested builds="
        + requested
        + "\nTotal time="
        + totalTime
        + "\nTotal time spent on builds="
        + buildTime;
  }

  /**
   * Starts timer and returns the exact time at call site.
   *
   * @return The time at executing this function.
   */
  public long startTimer() {
    return System.currentTimeMillis();
  }

  /**
   * Calculates the difference between the passed time and current time and adds it to time spent in
   * total time spent.
   *
   * @param timer The return result of calling {@link Log#startTimer()}.
   */
  public void stopTimerAndCapture(long timer) {
    this.totalTime += System.currentTimeMillis() - timer;
  }

  /**
   * Calculates the difference between the passed time and current time and adds it to time spent in
   * build time.
   *
   * @param timer The return result of calling {@link Log#startTimer()}.
   */
  public void stopTimerAndCaptureBuildTime(long timer) {
    this.buildTime += System.currentTimeMillis() - timer;
  }

  /** Increments the number of build requests. */
  public void incrementBuildRequest() {
    this.requested += 1;
  }

  /**
   * Adds the passed parameter to the number of {@link Log#nodes}.
   *
   * @param numberOfNewNodesCreated Number of new nodes created in {@link ConflictGraph}.
   */
  public void updateNodeNumber(long numberOfNewNodesCreated) {
    this.nodes += numberOfNewNodesCreated;
  }

  /**
   * Updates list of injected annotations with the latest injected annotations.
   *
   * @param annotations List of the latest injected annotations.
   */
  public void updateInjectedAnnotations(Set<AddAnnotation> annotations) {
    this.injectedAnnotations.addAll(annotations);
  }

  /**
   * Returns list of injected annotations.
   *
   * @return List of injected annotations.
   */
  public List<AddAnnotation> getInjectedAnnotations() {
    return injectedAnnotations;
  }
}
