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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimaps;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.ModuleInfo;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.injectors.AnnotationInjector;
import edu.ucr.cs.riple.core.injectors.VirtualInjector;
import edu.ucr.cs.riple.core.metadata.field.FieldDeclarationAnalysis;
import edu.ucr.cs.riple.core.metadata.index.Bank;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.method.MethodDeclarationTree;
import edu.ucr.cs.riple.core.metadata.method.MethodNode;
import edu.ucr.cs.riple.core.metadata.trackers.MethodRegionTracker;
import edu.ucr.cs.riple.core.metadata.trackers.RegionTracker;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.changes.AddAnnotation;
import edu.ucr.cs.riple.injector.location.OnMethod;
import edu.ucr.cs.riple.injector.location.OnParameter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;

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
  private final ImmutableMultimap<Integer, MethodStatus> methods;
  /** Annotator Config. */
  private final Config config;
  /** Method declaration tree instance. */
  private final MethodDeclarationTree tree;
  /**
   * VirtualInjector instance. Annotations should only be loaded in a library model and not
   * physically injected.
   */
  private final VirtualInjector injector;

  public DownStreamDependencyExplorer(Config config, MethodDeclarationTree tree) {
    this.config = config;
    this.modules = config.downstreamInfo;
    this.tree = tree;
    this.injector = new VirtualInjector(config);
    this.methods =
        Multimaps.index(
            tree.getPublicMethodsWithNonPrimitivesReturn().stream()
                .map(MethodStatus::new)
                .collect(ImmutableSet.toImmutableSet()),
            MethodStatus::hashCode);
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
        methods.values().stream()
            .filter(
                input ->
                    !tracker
                        .getCallersOfMethod(input.node.location.clazz, input.node.location.method)
                        .isEmpty()) // skip methods that are not called anywhere.
            .map(
                methodStatus ->
                    new Fix(
                        new AddAnnotation(
                            new OnMethod(
                                "null",
                                methodStatus.node.location.clazz,
                                methodStatus.node.location.method),
                            config.nullableAnnot),
                        "null",
                        "null",
                        "null",
                        true))
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
    OptimizedDownstreamDependencyAnalyzer explorer =
        new OptimizedDownstreamDependencyAnalyzer(
            injector, errorBank, fixBank, tracker, fixes, tree, 1, config);
    ImmutableSet<Report> reports = explorer.explore();
    // Update method status based on the results.
    methods
        .values()
        .forEach(
            method -> {
              MethodNode node = method.node;
              method.impactedParameters = explorer.getImpactedParameters(node.location);
              Optional<Report> optional =
                  reports.stream()
                      .filter(input -> input.root.toMethod().equals(node.location))
                      .findAny();
              optional.ifPresent(
                  report -> {
                    method.effect += report.localEffect;
                    method.triggeredErrors = report.triggeredErrors;
                  });
            });
    System.out.println("Analysing downstream dependencies completed!");
  }

  /**
   * Retrieves the corresponding {@link MethodStatus} to a fix.
   *
   * @param fix Target fix.
   * @return Corresponding {@link MethodStatus}, null if not located.
   */
  @Nullable
  private MethodStatus fetchStatus(Fix fix) {
    if (!fix.isOnMethod()) {
      return null;
    }
    OnMethod onMethod = fix.toMethod();
    int predictedHash = MethodStatus.hash(onMethod.method, onMethod.clazz);
    Optional<MethodStatus> optional =
        this.methods.get(predictedHash).stream()
            .filter(m -> m.node.location.equals(onMethod))
            .findAny();
    return optional.orElse(null);
  }

  /**
   * Returns the effect of applying a fix on the target on downstream dependencies.
   *
   * @param fix Fix targeting an element in target.
   * @return Effect on downstream dependencies.
   */
  private int effectOnDownstreamDependencies(Fix fix) {
    MethodStatus status = fetchStatus(fix);
    return status == null ? 0 : status.effect;
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

  /**
   * Returns set of parameters that will receive {@code @Nullable}, if any of the methods in the
   * fixTree are annotated as {@code @Nullable}.
   *
   * @param fixTree Fix tree.
   * @return Immutable set of impacted parameters.
   */
  public ImmutableSet<OnParameter> getImpactedParameters(Set<Fix> fixTree) {
    return fixTree.stream()
        .filter(Fix::isOnMethod)
        .flatMap(
            fix -> {
              MethodStatus status = fetchStatus(fix);
              return status == null ? Stream.of() : status.impactedParameters.stream();
            })
        .collect(ImmutableSet.toImmutableSet());
  }

  public List<Error> getTriggeredErrors(Fix fix) {
    if (!fix.isOnMethod()) {
      return Collections.emptyList();
    }
    OnMethod method = fix.toMethod();
    MethodNode node = tree.findNode(method);
    if (!node.isPublicMethodWithNonPrimitiveReturnType()) {
      return Collections.emptyList();
    }
    MethodStatus status = fetchStatus(fix);
    if (status == null) {
      return Collections.emptyList();
    }
    return status.triggeredErrors;
  }

  /** Container class for storing overall effect of each method. */
  private static class MethodStatus {
    /** Node in {@link MethodDeclarationTree} corresponding to a public method. */
    final MethodNode node;
    /**
     * Set of parameters in target module that will receive {@code Nullable} value if targeted
     * method in node is annotated as {@code @Nullable}.
     */
    public Set<OnParameter> impactedParameters;

    /**
     * List of triggered errors in downstream dependencies if method is annotated as {@code
     * Nullable}.
     */
    public ImmutableList<Error> triggeredErrors;
    /**
     * Effect of injecting a {@code Nullable} annotation on pointing method of node on downstream
     * dependencies.
     */
    int effect;

    public MethodStatus(MethodNode node) {
      this.node = node;
      this.effect = 0;
    }

    @Override
    public int hashCode() {
      return hash(node.location.method, node.location.clazz);
    }

    /**
     * Calculates hash. This method is used outside this class to calculate the expected hash based
     * on instance's properties value if the actual instance is not available.
     *
     * @param method Method signature.
     * @param clazz Fully qualified name of the containing class.
     * @return Expected hash.
     */
    public static int hash(String method, String clazz) {
      return MethodNode.hash(method, clazz);
    }
  }

  /** Explorer for analyzing downstream dependencies. */
  private static class OptimizedDownstreamDependencyAnalyzer extends OptimizedExplorer {

    /**
     * Map of public methods in target module to parameters in target module, which are source of
     * nullable flow back to upstream module (target) from downstream dependencies, if annotated as
     * {@code @Nullable}.
     */
    private final HashMap<OnMethod, Set<OnParameter>> nullableFlowMap;

    public OptimizedDownstreamDependencyAnalyzer(
        AnnotationInjector injector,
        Bank<Error> errorBank,
        Bank<Fix> fixBank,
        RegionTracker tracker,
        ImmutableSet<Fix> fixes,
        MethodDeclarationTree methodDeclarationTree,
        int depth,
        Config config) {
      super(injector, errorBank, fixBank, tracker, fixes, methodDeclarationTree, depth, config);
      this.nullableFlowMap = new HashMap<>();
    }

    @Override
    public void rerunAnalysis() {
      Utility.buildDownstreamDependencies(config);
    }

    @Override
    protected void finalizeReports() {
      super.finalizeReports();
      // Collect impacted parameters in target module by downstream dependencies.
      graph
          .getNodes()
          .forEach(
              node ->
                  node.root.ifOnMethod(
                      method -> {
                        // Impacted parameters.
                        Set<OnParameter> parameters =
                            node.triggeredErrors.stream()
                                .filter(
                                    error ->
                                        error.nonnullTarget != null
                                            && error.nonnullTarget.isOnParameter()
                                            // Method is declared in the target module.
                                            && methodDeclarationTree.declaredInModule(
                                                error.nonnullTarget.toMethod()))
                                .map(error -> error.nonnullTarget.toParameter())
                                .collect(Collectors.toSet());
                        if (!parameters.isEmpty()) {
                          // Update uri for each parameter. These triggered fixes does not have an
                          // actual physical uri since they are provided as a jar file in downstream
                          // dependencies.
                          parameters.forEach(
                              onParameter ->
                                  onParameter.uri =
                                      methodDeclarationTree.findNode(
                                              onParameter.method, onParameter.clazz)
                                          .location
                                          .uri);
                          nullableFlowMap.put(method, parameters);
                        }
                      }));
    }

    /**
     * Returns set of parameters that will receive {@code @Nullable} if the passed method is
     * annotated as {@code @Nullable}.
     *
     * @param method Method to be annotated.
     * @return Set of impacted parameters. If no parameter is impacted, empty set will be returned.
     */
    private Set<OnParameter> getImpactedParameters(OnMethod method) {
      return nullableFlowMap.getOrDefault(method, Collections.emptySet());
    }
  }
}
