/*
 * MIT License
 *
 * Copyright (c) 2020 anonymous
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

package com.example.tool.core.evaluators.graphprocessor;

import com.example.tool.core.Config;
import com.example.tool.core.cache.downstream.DownstreamImpactCache;
import com.example.tool.core.evaluators.suppliers.Supplier;
import com.example.tool.core.injectors.AnnotationInjector;
import com.example.tool.core.metadata.graph.Node;
import com.example.tool.core.metadata.index.ErrorStore;
import com.example.tool.core.metadata.index.Fix;
import com.example.tool.core.metadata.method.MethodDeclarationTree;
import com.example.tool.injector.changes.AddMarkerAnnotation;
import com.example.tool.injector.location.Location;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Base class for conflict graph processors. */
public abstract class AbstractConflictGraphProcessor implements ConflictGraphProcessor {

  /** Method declaration tree. */
  protected final MethodDeclarationTree methodDeclarationTree;
  /** Injector used in the processor to inject / remove fixes. */
  protected final AnnotationInjector injector;
  /** Error store instance to store state of fixes before and after of injections. */
  protected final ErrorStore errorStore;
  /** Downstream impact cache to retrieve impacts of fixes globally. */
  protected final DownstreamImpactCache downstreamImpactCache;
  /** Annotator config. */
  protected final Config config;
  /** Handler to re-run compiler. */
  protected final CompilerRunner compilerRunner;

  public AbstractConflictGraphProcessor(Config config, CompilerRunner runner, Supplier supplier) {
    this.config = config;
    this.methodDeclarationTree = supplier.getMethodDeclarationTree();
    this.injector = supplier.getInjector();
    this.downstreamImpactCache = supplier.getDownstreamImpactCache();
    this.errorStore = supplier.getErrorStore();
    this.compilerRunner = runner;
  }

  /**
   * Get set of triggered fixes from downstream dependencies.
   *
   * @param node Node in process.
   */
  protected Set<Fix> getTriggeredFixesFromDownstream(Node node) {
    Set<Location> currentLocationsTargetedByTree =
        node.tree.stream()
            .map(Fix::toLocation)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    return downstreamImpactCache.getTriggeredErrorsForCollection(node.tree).stream()
        .filter(
            error ->
                error.isSingleFix()
                    && error.toResolvingLocation().isOnParameter()
                    && error.isFixableOnTarget(methodDeclarationTree)
                    && !currentLocationsTargetedByTree.contains(error.toResolvingLocation()))
        .map(
            error ->
                new Fix(
                    new AddMarkerAnnotation(
                        error.toResolvingLocation().toParameter(), config.nullableAnnot),
                    "PASSING_NULLABLE",
                    false))
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }
}