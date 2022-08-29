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

package edu.ucr.cs.riple.core.explorers;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.ModuleInfo;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.injectors.AnnotationInjector;
import edu.ucr.cs.riple.core.injectors.VirtualInjector;
import edu.ucr.cs.riple.core.metadata.field.FieldDeclarationAnalysis;
import edu.ucr.cs.riple.core.metadata.index.Bank;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.method.MethodInheritanceTree;
import edu.ucr.cs.riple.core.metadata.method.MethodNode;
import edu.ucr.cs.riple.core.metadata.trackers.MethodRegionTracker;
import edu.ucr.cs.riple.core.metadata.trackers.RegionTracker;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.changes.AddAnnotation;
import edu.ucr.cs.riple.injector.location.OnMethod;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Analyzer for downstream dependencies.
 *
 * <p>This class analyzes switching the nullability of public APIs of one compilation target, in
 * order to compute the effect on said target's downstream dependencies of adding annotations to
 * those APIs. It does so by building said downstream dependencies. It collects the effect (number
 * of additional errors) of each such change, by summing across all downstream dependencies. This
 * data then be fed to the Annotator main process in the decision process.
 */
public class DownStreamDependencyExplorer {

  /** Set of downstream dependencies. */
  private final ImmutableSet<ModuleInfo> modules;
  /** Public APIs in the target modules that have a non-primitive return value. */
  private final ImmutableSet<MethodStatus> methods;
  /** Annotator Config. */
  private final Config config;
  /** Method inheritance instance. */
  private final MethodInheritanceTree tree;
  /**
   * VirtualInjector instance. Annotations should only be loaded in a library model and not
   * physically injected.
   */
  private final VirtualInjector injector;

  public DownStreamDependencyExplorer(Config config, MethodInheritanceTree tree) {
    this.config = config;
    this.modules = config.downstreamInfo;
    this.tree = tree;
    this.injector = new VirtualInjector(config);
    this.methods =
        tree.getPublicMethodsWithNonPrimitivesReturn().stream()
            .map(MethodStatus::new)
            .collect(ImmutableSet.toImmutableSet());
  }

  /**
   * Exploration phase begins. When this method finishes, all methods effect on downstream
   * dependencies are calculated.
   */
  public void explore() {
    System.out.println("Analysing downstream dependencies...");
    Utility.setScannerCheckerActivation(modules, true);
    Utility.buildDownstreamDependencies(config);
    Utility.setScannerCheckerActivation(modules, false);
    // Collect callers of public APIs in module.
    MethodRegionTracker tracker = new MethodRegionTracker(config.downstreamInfo, tree);
    // Generate fixes corresponding methods.
    ImmutableSet<Fix> fixes =
        methods.stream()
            .filter(
                input ->
                    !tracker
                        .getCallersOfMethod(input.node.clazz, input.node.method)
                        .isEmpty()) // skip methods that are not called anywhere.
            .map(
                methodStatus ->
                    new Fix(
                        new AddAnnotation(
                            new OnMethod("null", methodStatus.node.clazz, methodStatus.node.method),
                            config.nullableAnnot),
                        "null",
                        "null",
                        "null"))
            .collect(ImmutableSet.toImmutableSet());
    // Explorer initializations.
    FieldDeclarationAnalysis fieldDeclarationAnalysis =
        new FieldDeclarationAnalysis(config.downstreamInfo);
    Bank<Error> errorBank =
        new Bank<>(
            config.downstreamInfo.stream()
                .map(info -> info.dir.resolve("errors.tsv"))
                .collect(ImmutableSet.toImmutableSet()),
            Error::new);
    Bank<Fix> fixBank =
        new Bank<>(
            config.downstreamInfo.stream()
                .map(info -> info.dir.resolve("fixes.tsv"))
                .collect(ImmutableSet.toImmutableSet()),
            Fix.factory(config, fieldDeclarationAnalysis));
    Explorer explorer =
        new OptimizedDownstreamDependencyAnalyzer(
            injector, errorBank, fixBank, tracker, fixes, tree, 1, config);
    ImmutableSet<Report> reports = explorer.explore();
    // Update method status based on the results.
    methods.forEach(
        method -> {
          MethodNode node = method.node;
          Optional<Report> optional =
              reports.stream()
                  .filter(
                      input ->
                          input.root.toMethod().method.equals(node.method)
                              && input.root.toMethod().clazz.equals(node.clazz))
                  .findAny();
          optional.ifPresent(report -> method.effect += report.localEffect);
        });
    System.out.println("Analysing downstream dependencies completed!");
  }

  /**
   * Returns the effect of applying a fix on the target on downstream dependencies.
   *
   * @param fix Fix targeting an element in target.
   * @return Effect on downstream dependencies.
   */
  private int effectOnDownstreamDependencies(Fix fix) {
    if (!fix.isOnMethod()) {
      return 0;
    }
    OnMethod onMethod = fix.toMethod();
    Optional<MethodStatus> optional =
        this.methods.stream()
            .filter(
                m -> m.node.method.equals(onMethod.method) && m.node.clazz.equals(onMethod.clazz))
            .findAny();
    return optional.map(methodStatus -> methodStatus.effect).orElse(0);
  }

  /**
   * Returns the lower bound of number of errors of applying a fix and its associated chain of fixes
   * on the target on downstream dependencies.
   *
   * @param tree Tree of the fix tree associated to root.
   * @return Lower bound of number of errors on downstream dependencies.
   */
  public int computeLowerBoundOfNumberOfErrors(Set<Fix> tree) {
    OptionalInt lowerBoundEffectOfChainOptional =
        tree.stream().mapToInt(this::effectOnDownstreamDependencies).max();
    if (lowerBoundEffectOfChainOptional.isEmpty()) {
      return 0;
    }
    return lowerBoundEffectOfChainOptional.getAsInt();
  }

  /**
   * Returns the upper bound of number of errors of applying a fix and its associated chain of fixes
   * on the target on downstream dependencies.
   *
   * @param tree Tree of the fix tree associated to root.
   * @return Lower bound of number of errors on downstream dependencies.
   */
  public int computeUpperBoundOfNumberOfErrors(Set<Fix> tree) {
    return tree.stream().mapToInt(this::effectOnDownstreamDependencies).sum();
  }

  /** Container class for storing overall effect of each method. */
  private static class MethodStatus {
    /** Node in {@link MethodInheritanceTree} corresponding to a public method. */
    final MethodNode node;
    /**
     * Effect of injecting a {@code Nullable} annotation on pointing method of node on downstream
     * dependencies.
     */
    int effect;

    public MethodStatus(MethodNode node) {
      this.node = node;
      this.effect = 0;
    }
  }

  /** Explorer for analyzing downstream dependencies. */
  private static class OptimizedDownstreamDependencyAnalyzer extends OptimizedExplorer {

    public OptimizedDownstreamDependencyAnalyzer(
        AnnotationInjector injector,
        Bank<Error> errorBank,
        Bank<Fix> fixBank,
        RegionTracker tracker,
        ImmutableSet<Fix> fixes,
        MethodInheritanceTree methodInheritanceTree,
        int depth,
        Config config) {
      super(injector, errorBank, fixBank, tracker, fixes, methodInheritanceTree, depth, config);
    }

    @Override
    public void rerunAnalysis() {
      Utility.buildDownstreamDependencies(config);
    }
  }
}
