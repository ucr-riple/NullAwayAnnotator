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
import com.example.tool.core.cache.TargetModuleCache;
import com.example.tool.core.cache.downstream.DownstreamImpactCache;
import com.example.tool.core.evaluators.graphprocessor.CompilerRunner;
import com.example.tool.core.evaluators.graphprocessor.ConflictGraphProcessor;
import com.example.tool.core.evaluators.graphprocessor.ParallelConflictGraphProcessor;
import com.example.tool.core.evaluators.graphprocessor.SequentialConflictGraphProcessor;
import com.example.tool.core.injectors.AnnotationInjector;
import com.example.tool.core.injectors.PhysicalInjector;
import com.example.tool.core.metadata.method.MethodDeclarationTree;
import com.example.tool.core.metadata.trackers.CompoundTracker;
import com.example.tool.core.metadata.trackers.RegionTracker;
import com.example.tool.core.util.Utility;
import com.google.common.collect.ImmutableSet;

/**
 * Supplier for target module analysis. It has the following characteristics:
 *
 * <ul>
 *   <li>Annotations are physically injected on target module.
 *   <li>Analysis is performed to depth set in config.
 *   <li>Depending on the config, global impact of annotations can be considered.
 * </ul>
 */
public class TargetModuleSupplier extends AbstractSupplier {

  protected final DownstreamImpactCache downstreamImpactCache;
  protected final TargetModuleCache targetModuleCache;

  /**
   * Constructor for target module supplier instance.
   *
   * @param config Annotator config instance.
   * @param targetModuleCache Target module impact cache instance.
   * @param downstreamImpactCache Downstream impact cache instance.
   * @param tree Method declaration tree for methods in target module.
   */
  public TargetModuleSupplier(
      Config config,
      TargetModuleCache targetModuleCache,
      DownstreamImpactCache downstreamImpactCache,
      MethodDeclarationTree tree) {
    super(ImmutableSet.of(config.target), config, tree);
    this.downstreamImpactCache = downstreamImpactCache;
    this.targetModuleCache = targetModuleCache;
  }

  @Override
  protected AnnotationInjector initializeInjector() {
    return new PhysicalInjector(config);
  }

  @Override
  protected int initializeDepth() {
    return config.depth;
  }

  @Override
  public DownstreamImpactCache getDownstreamImpactCache() {
    return downstreamImpactCache;
  }

  @Override
  public ConflictGraphProcessor getGraphProcessor() {
    CompilerRunner runner = () -> Utility.buildTarget(config);
    if (config.useParallelGraphProcessor) {
      RegionTracker tracker = new CompoundTracker(config, config.target, tree);
      return new ParallelConflictGraphProcessor(config, runner, this, tracker);
    }
    return new SequentialConflictGraphProcessor(config, runner, this);
  }

  @Override
  public TargetModuleCache getCache() {
    return targetModuleCache;
  }
}