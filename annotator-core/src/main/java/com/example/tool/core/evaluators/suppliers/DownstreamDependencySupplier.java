/*
 * MIT License
 *
 * Copyright (c) 2022 anonymous
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

package com.example.tool.core.evaluators.suppliers;

import com.example.tool.core.Config;
import com.example.tool.core.cache.downstream.DownstreamImpactCache;
import com.example.tool.core.cache.downstream.VoidDownstreamImpactCache;
import com.example.tool.core.evaluators.graphprocessor.CompilerRunner;
import com.example.tool.core.evaluators.graphprocessor.ParallelConflictGraphProcessor;
import com.example.tool.core.evaluators.graphprocessor.SequentialConflictGraphProcessor;
import com.example.tool.core.injectors.AnnotationInjector;
import com.example.tool.core.injectors.VirtualInjector;
import com.example.tool.core.metadata.method.MethodDeclarationTree;
import com.example.tool.core.metadata.trackers.RegionTracker;
import com.example.tool.core.util.Utility;
import com.example.tool.core.cache.TargetModuleCache;
import com.example.tool.core.evaluators.graphprocessor.AbstractConflictGraphProcessor;

/**
 * Supplier for downstream dependency analysis. It has the following characteristics:
 *
 * <ul>
 *   <li>Annotations are virtually injected on downstream dependencies.
 *   <li>Analysis is performed only to depth 1.
 *   <li>Global impact of annotations are neglected.
 * </ul>
 */
public class DownstreamDependencySupplier extends AbstractSupplier {

  private final RegionTracker tracker;

  public DownstreamDependencySupplier(
          Config config, RegionTracker tracker, MethodDeclarationTree tree) {
    super(config.downstreamInfo, config, tree);
    this.tracker = tracker;
  }

  @Override
  protected AnnotationInjector initializeInjector() {
    return new VirtualInjector(config);
  }

  @Override
  protected int initializeDepth() {
    return 1;
  }

  @Override
  public DownstreamImpactCache getDownstreamImpactCache() {
    return new VoidDownstreamImpactCache();
  }

  @Override
  public AbstractConflictGraphProcessor getGraphProcessor() {
    CompilerRunner runner = () -> Utility.buildDownstreamDependencies(config);
    return config.useParallelGraphProcessor
        ? new ParallelConflictGraphProcessor(config, runner, this, tracker)
        : new SequentialConflictGraphProcessor(config, runner, this);
  }

  @Override
  public TargetModuleCache getCache() {
    throw new RuntimeException(
        "Caches are used to retrieve impacts for depths more than 1. Downstream dependency analysis happens only at depth 1.");
  }
}
