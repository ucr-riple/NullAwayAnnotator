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

package edu.ucr.cs.riple.core.evaluators.suppliers;

import edu.ucr.cs.riple.core.Context;
import edu.ucr.cs.riple.core.cache.TargetModuleCache;
import edu.ucr.cs.riple.core.cache.downstream.DownstreamImpactCache;
import edu.ucr.cs.riple.core.cache.downstream.VoidDownstreamImpactCache;
import edu.ucr.cs.riple.core.evaluators.graph.processors.AbstractConflictGraphProcessor;
import edu.ucr.cs.riple.core.evaluators.graph.processors.CompilerRunner;
import edu.ucr.cs.riple.core.evaluators.graph.processors.ParallelConflictGraphProcessor;
import edu.ucr.cs.riple.core.evaluators.graph.processors.SequentialConflictGraphProcessor;
import edu.ucr.cs.riple.core.injectors.AnnotationInjector;
import edu.ucr.cs.riple.core.injectors.VirtualInjector;
import edu.ucr.cs.riple.core.metadata.region.MethodRegionRegistry;
import edu.ucr.cs.riple.core.metadata.region.RegionRegistry;
import edu.ucr.cs.riple.core.module.ModuleInfo;
import edu.ucr.cs.riple.core.util.Utility;

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

  /**
   * Region registry for downstream dependency analysis. It is used to locate regions where a fix on
   * a public method with non-primitive return type can introduce new errors.
   */
  private final RegionRegistry regionRegistry;

  public DownstreamDependencySupplier(Context context) {
    super(
        context,
        new ModuleInfo(context, context.downstreamConfigurations, context.config.buildCommand));
    this.regionRegistry = new MethodRegionRegistry(moduleInfo);
  }

  @Override
  protected AnnotationInjector initializeInjector() {
    return new VirtualInjector(context);
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
    CompilerRunner runner = () -> Utility.buildDownstreamDependencies(context);
    return context.config.useParallelGraphProcessor
        ? new ParallelConflictGraphProcessor(context, runner, this, regionRegistry)
        : new SequentialConflictGraphProcessor(context, runner, this);
  }

  @Override
  public TargetModuleCache getTargetModuleCache() {
    throw new RuntimeException(
        "Caches are used to retrieve impacts for depths more than 1. Downstream dependency analysis happens only at depth 1.");
  }

  @Override
  public ModuleInfo getModuleInfo() {
    return moduleInfo;
  }
}
