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
import edu.ucr.cs.riple.core.evaluators.graph.processors.CompilerRunner;
import edu.ucr.cs.riple.core.evaluators.graph.processors.ConflictGraphProcessor;
import edu.ucr.cs.riple.core.evaluators.graph.processors.ParallelConflictGraphProcessor;
import edu.ucr.cs.riple.core.evaluators.graph.processors.SequentialConflictGraphProcessor;
import edu.ucr.cs.riple.core.injectors.AnnotationInjector;
import edu.ucr.cs.riple.core.injectors.PhysicalInjector;
import edu.ucr.cs.riple.core.module.ModuleInfo;
import edu.ucr.cs.riple.core.util.Utility;

/**
 * Supplier for target module analysis. It has the following characteristics:
 *
 * <ul>
 *   <li>Annotations are physically injected on target module.
 *   <li>Analysis is performed to depth set in context.
 *   <li>Depending on the context, global impact of annotations can be considered.
 * </ul>
 */
public class TargetModuleSupplier extends AbstractSupplier {

  protected final DownstreamImpactCache downstreamImpactCache;
  protected final TargetModuleCache targetModuleCache;

  /**
   * Constructor for target module supplier instance.
   *
   * @param context Annotator context instance.
   * @param targetModuleCache Target module impact cache instance.
   * @param downstreamImpactCache Downstream impact cache instance.
   */
  public TargetModuleSupplier(
      Context context,
      TargetModuleCache targetModuleCache,
      DownstreamImpactCache downstreamImpactCache) {
    super(context, context.targetModuleInfo);
    this.downstreamImpactCache = downstreamImpactCache;
    this.targetModuleCache = targetModuleCache;
  }

  @Override
  protected AnnotationInjector initializeInjector() {
    return new PhysicalInjector(context);
  }

  @Override
  protected int initializeDepth() {
    return context.config.depth;
  }

  @Override
  public DownstreamImpactCache getDownstreamImpactCache() {
    return downstreamImpactCache;
  }

  @Override
  public ConflictGraphProcessor getGraphProcessor() {
    CompilerRunner runner = () -> Utility.buildTarget(context);
    if (context.config.useParallelGraphProcessor) {
      return new ParallelConflictGraphProcessor(context, runner, this);
    }
    return new SequentialConflictGraphProcessor(context, runner, this);
  }

  @Override
  public TargetModuleCache getTargetModuleCache() {
    return targetModuleCache;
  }

  @Override
  public ModuleInfo getModuleInfo() {
    return moduleInfo;
  }
}
