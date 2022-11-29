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

package edu.ucr.cs.riple.core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import edu.ucr.cs.riple.core.global.GlobalAnalyzer;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.injector.location.Location;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Container class to store information regarding effectiveness of a fix, its associated fix tree
 * and impacts on downstream dependencies.
 */
public class Report {

  /** Effect on target module. */
  public int localEffect;
  /** Root of fix tree associated to this report instance. */
  public Fix root;
  /** Fix tree associated to this report instance. */
  public Set<Fix> tree;
  /**
   * Set of fixes that will be triggered in target module if fix tree is applied to the source code.
   */
  public ImmutableSet<Fix> triggeredFixes;
  /**
   * Set of fixes that will be triggered in target module if fix tree is applied to the source code.
   */
  public ImmutableList<Error> triggeredErrors;
  /** If true, all leaves of fix tree are not resolvable by any {@code @Nullable} annotation. */
  public boolean finished;
  /**
   * Lower bound of number of errors in downstream dependencies if fix tree is applied to the target
   * module.
   */
  private int lowerBoundEffectOnDownstreamDependencies;
  /**
   * Upper bound of number of errors in downstream dependencies if fix tree is applied to the target
   * module.
   */
  private int upperBoundEffectOnDownstreamDependencies;

  /** Denotes the final decision regarding the injection of the report. */
  public enum Tag {
    /** If tagged with this tag, report tree will be injected. */
    APPROVE,
    /** If tagged with this tag, report tree will not be injected and will be discarded. */
    REJECT,
  }

  /** Status of the report. */
  private Tag tag;

  public Report(Fix root, int localEffect) {
    this.localEffect = localEffect;
    this.root = root;
    this.tree = Sets.newHashSet(root);
    this.finished = false;
    this.triggeredFixes = ImmutableSet.of();
    this.triggeredErrors = ImmutableList.of();
    this.lowerBoundEffectOnDownstreamDependencies = 0;
    this.upperBoundEffectOnDownstreamDependencies = 0;
    this.tag = Tag.REJECT;
  }

  /**
   * Checks if any of the fix in tree, will trigger an unresolvable error in downstream
   * dependencies.
   *
   * @param analyzer Analyzer to check impact of method.
   * @return true, if report contains a fix which will trigger an unresolvable error in downstream
   *     dependency.
   */
  public boolean containsDestructiveMethod(GlobalAnalyzer analyzer) {
    return this.tree.stream().anyMatch(analyzer::isNotFixableOnTarget);
  }

  /**
   * Setter for tag.
   *
   * @param tag tag value.
   */
  public void tag(Tag tag) {
    this.tag = tag;
  }

  /**
   * Getter for tag.
   *
   * @return Reports tag.
   */
  public Tag getTag() {
    return this.tag;
  }

  /**
   * Checks if report's fix tree is approved by analysis and should be applied.
   *
   * @return true, if fix tree should be applied and false otherwise.
   */
  public boolean approved() {
    return this.tag.equals(Tag.APPROVE);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof Report)) {
      return false;
    }
    Report report = (Report) other;
    return root.equals(report.root);
  }

  @Override
  public int hashCode() {
    return Objects.hash(root);
  }

  /**
   * Mainly used for unit tests, what we care in tests, is that if a fix has the correct overall
   * effectiveness with all the corresponding fixes to reach that effectiveness, therefore only the
   * locations are compared in trees.
   *
   * @param config Annotator Config instance.
   * @param other Other report, mainly coming from tests.
   * @return true, if two reports are equal (same effectiveness and all locations)
   */
  public boolean testEquals(Config config, Report other) {
    if (!this.root.equals(other.root)) {
      return false;
    }
    if (this.getOverallEffect(config) != other.getOverallEffect(config)) {
      return false;
    }
    this.tree.add(this.root);
    other.tree.add(other.root);
    Set<Location> thisTree = this.tree.stream().map(Fix::toLocation).collect(Collectors.toSet());
    Set<Location> otherTree = other.tree.stream().map(Fix::toLocation).collect(Collectors.toSet());
    if (!thisTree.equals(otherTree)) {
      return false;
    }
    Set<Location> thisTriggered =
        this.triggeredFixes.stream().map(Fix::toLocation).collect(Collectors.toSet());
    Set<Location> otherTriggered =
        other.triggeredFixes.stream().map(Fix::toLocation).collect(Collectors.toSet());
    return otherTriggered.equals(thisTriggered);
  }

  @Override
  public String toString() {
    return "Effect="
        + localEffect
        + ", "
        + root
        + ", "
        + tree.stream().map(Fix::toLocation).collect(Collectors.toSet());
  }

  /**
   * Computes the boundaries of effectiveness of applying the fix tree to target module on
   * downstream dependencies.
   *
   * @param explorer Downstream dependency instance.
   */
  public void computeBoundariesOfEffectivenessOnDownstreamDependencies(GlobalAnalyzer explorer) {
    this.lowerBoundEffectOnDownstreamDependencies =
        explorer.computeLowerBoundOfNumberOfErrors(tree);
    this.upperBoundEffectOnDownstreamDependencies =
        explorer.computeUpperBoundOfNumberOfErrors(tree);
  }

  /**
   * Returns the overall effect of applying fix tree associated to this report according to {@link
   * AnalysisMode}.
   *
   * @param config Annotator config.
   * @return Overall effect ot applying the fix tree.
   */
  public int getOverallEffect(Config config) {
    AnalysisMode mode = config.mode;
    if (mode.equals(AnalysisMode.LOCAL)) {
      return this.localEffect;
    }
    if (mode.equals(AnalysisMode.UPPER_BOUND)) {
      return this.localEffect + this.upperBoundEffectOnDownstreamDependencies;
    }
    return this.localEffect + this.lowerBoundEffectOnDownstreamDependencies;
  }

  /**
   * Getter for lower bound effect on downstream dependencies.
   *
   * @return lowerBoundEffectOnDownstreamDependencies
   */
  public int getLowerBoundEffectOnDownstreamDependencies() {
    return lowerBoundEffectOnDownstreamDependencies;
  }

  /**
   * Getter for upper bound effect on downstream dependencies.
   *
   * @return upperBoundEffectOnDownstreamDependencies
   */
  public int getUpperBoundEffectOnDownstreamDependencies() {
    return upperBoundEffectOnDownstreamDependencies;
  }

  /**
   * Checks if the report needs further investigation. If a fix is suggested from downstream
   * dependencies, it should still be included the next cycle.
   *
   * @param config Annotator config instance.
   * @return true, if report needs further investigation.
   */
  public boolean isInProgress(Config config) {
    return (!finished && (!config.bailout || localEffect > 0))
        || triggeredFixes.stream().anyMatch(input -> !input.fixSourceIsInTarget);
  }
}