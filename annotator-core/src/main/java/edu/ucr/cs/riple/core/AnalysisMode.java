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

import static edu.ucr.cs.riple.core.Report.Tag.APPROVE;
import static edu.ucr.cs.riple.core.Report.Tag.REJECT;
import static edu.ucr.cs.riple.core.util.Utility.log;

import com.google.common.base.Preconditions;
import edu.ucr.cs.riple.core.cache.downstream.DownstreamImpactCache;
import edu.ucr.cs.riple.core.cache.downstream.DownstreamImpactCacheImpl;
import java.util.Collection;

/** Analysis mode in making inference decisions. */
public enum AnalysisMode {
  /**
   * Only effects in target module is considered. Default mode if downstream dependencies analysis
   * is not activated.
   */
  LOCAL {
    @Override
    public void tag(DownstreamImpactCache downstreamImpactCache, Collection<Report> reports) {
      reports.forEach(
          report -> {
            if (report.localEffect < 1) {
              report.tag(APPROVE);
            } else {
              report.tag(REJECT);
            }
          });
    }
  },

  /**
   * Guarantees that no error will happen on downstream dependencies in the result of changes in
   * upstream.
   */
  STRICT {
    @Override
    public void tag(DownstreamImpactCache downstreamImpactCache, Collection<Report> reports) {
      reports.forEach(
          report -> {
            Annotator.logActive =
                !report.containsDestructiveMethod(downstreamImpactCache)
                    && report.getUpperBoundEffectOnDownstreamDependencies() != 0;
            log(
                "===================================UNEXPECTED STATE===================================");
            log("Upper bound value: " + report.getUpperBoundEffectOnDownstreamDependencies());
            final int[] newUpperBoundCalc = {0};
            log("Upper bound calculation:");
            report.tree.forEach(
                fix -> {
                  log("Fix location: " + fix.toLocation());
                  boolean triggersUnresolvableErrorsOnDownstream =
                      downstreamImpactCache.triggersUnresolvableErrorsOnDownstream(fix);
                  log(
                      "Triggers unresolved errors on downstream: "
                          + triggersUnresolvableErrorsOnDownstream);
                  if (triggersUnresolvableErrorsOnDownstream) {
                    log("UNEXPECTED STATE: Expected to not trigger any error");
                  }
                  log("computing effectOnDownstreamDependencies:");
                  int upperEffect =
                      ((DownstreamImpactCacheImpl) downstreamImpactCache)
                          .effectOnDownstreamDependencies(fix, report.tree);
                  log("newUpperBoundCalc[0] before: " + newUpperBoundCalc[0]);
                  newUpperBoundCalc[0] += upperEffect;
                  log("Effect on downstream:" + upperEffect);
                  log("newUpperBoundCalc[0] after: " + newUpperBoundCalc[0]);
                  if (upperEffect != 0) {
                    log("UNEXPECTED STATE: " + upperEffect);
                  }
                });
            log("Recomputed value of upper bound: " + newUpperBoundCalc[0]);

            log("checking containsDestructiveMethod:");
            boolean containsDestructiveMethod =
                report.containsDestructiveMethod(downstreamImpactCache);
            log("containsDestructiveMethod: " + containsDestructiveMethod);
            log("===================================END===================================");
            Annotator.logActive = false;
            // Check for destructive methods.
            if (report.containsDestructiveMethod(downstreamImpactCache)) {
              report.tag(REJECT);
              return;
            }
            // Just a sanity check.
            Preconditions.checkArgument(report.getUpperBoundEffectOnDownstreamDependencies() == 0);
            // Apply if effect is less than 1.
            if (report.localEffect < 1) {
              report.tag(APPROVE);
              return;
            }
            // Discard.
            report.tag(REJECT);
          });
    }
  },

  /**
   * Experimental: Upper bound of number of errors on downstream dependencies will be considered.
   */
  UPPER_BOUND {
    @Override
    public void tag(DownstreamImpactCache downstreamImpactCache, Collection<Report> reports) {
      reports.forEach(
          report -> {
            if (report.localEffect + report.getUpperBoundEffectOnDownstreamDependencies() < 1) {
              report.tag(APPROVE);
            } else {
              report.tag(REJECT);
            }
          });
    }
  },

  /**
   * Lower bound of number of errors on downstream dependencies will be considered. Default mode if
   * downstream dependencies analysis is enabled.
   */
  LOWER_BOUND {
    @Override
    public void tag(DownstreamImpactCache downstreamImpactCache, Collection<Report> reports) {
      reports.forEach(
          report -> {
            if (report.localEffect + report.getLowerBoundEffectOnDownstreamDependencies() < 1) {
              report.tag(APPROVE);
            } else {
              report.tag(REJECT);
            }
          });
    }
  };

  /**
   * Tags reports based on the analysis mode.
   *
   * @param downstreamImpactCache Downstream dependency instance.
   * @param reports Reports to be processed.
   */
  public abstract void tag(DownstreamImpactCache downstreamImpactCache, Collection<Report> reports);

  /**
   * Parses the received option and returns the corresponding {@link AnalysisMode}. Can only be one
   * of [default|upper_bound|lower_bound|strict] values.
   *
   * @param downStreamDependenciesAnalysisActivated if true, downstream dependency analysis is
   *     activated.
   * @param mode passed mode.
   * @return the corresponding {@link AnalysisMode}.
   */
  public static AnalysisMode parseMode(
      boolean downStreamDependenciesAnalysisActivated, String mode) {
    if (!downStreamDependenciesAnalysisActivated) {
      return LOCAL;
    }
    mode = mode.toLowerCase();
    if (mode.equals("lower_bound") || mode.equals("default")) {
      return LOWER_BOUND;
    }
    if (mode.equals("upper_bound")) {
      return UPPER_BOUND;
    }
    if (mode.equals("strict")) {
      return STRICT;
    }
    throw new IllegalArgumentException(
        "Unrecognized mode request: "
            + mode
            + " .Can only be [default|upper_bound|lower_bound|strict].");
  }
}
