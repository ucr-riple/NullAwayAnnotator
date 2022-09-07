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

package edu.ucr.cs.riple.core.tools;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.injector.changes.AddAnnotation;
import edu.ucr.cs.riple.injector.location.Location;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/** Wrapper class for {@link Report} with default values, used to create tests. */
public class TReport extends Report {

  private final int expectedValue;

  public TReport(Location root, int effect) {
    this(root, effect, "", "");
  }

  public TReport(Location root, int effect, String encClass, String encMethod) {
    super(
        new Fix(
            new AddAnnotation(root, "javax.annotation.Nullable"), null, encClass, encMethod, true),
        effect);
    this.expectedValue = effect;
  }

  public TReport(
      Location root,
      int effect,
      @Nullable Set<Location> addToTree,
      @Nullable Set<Location> triggered) {
    this(root, effect);
    if (triggered != null) {
      this.triggeredFixes =
          triggered.stream()
              .map((Function<Location, Fix>) TFix::new)
              .collect(ImmutableSet.toImmutableSet());
    }
    if (addToTree != null) {
      this.tree.addAll(
          addToTree.stream().map((Function<Location, Fix>) TFix::new).collect(Collectors.toSet()));
    }
  }

  /**
   * Returns the target value which is under examination under tests.
   *
   * @return expected value for the property under examination.
   */
  public int getExpectedValue() {
    return this.expectedValue;
  }

  @Override
  public int getOverallEffect(Config config) {
    return expectedValue;
  }
}
