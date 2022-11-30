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

package edu.ucr.cs.riple.core.explorers.suppliers;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.explorers.graphprocessor.CompilerRunner;
import edu.ucr.cs.riple.core.explorers.graphprocessor.ConflictGraphProcessor;
import edu.ucr.cs.riple.core.explorers.graphprocessor.ParallelConflictGraphProcessor;
import edu.ucr.cs.riple.core.explorers.graphprocessor.SequentialConflictGraphProcessor;
import edu.ucr.cs.riple.core.global.GlobalAnalyzer;
import edu.ucr.cs.riple.core.injectors.AnnotationInjector;
import edu.ucr.cs.riple.core.injectors.PhysicalInjector;
import edu.ucr.cs.riple.core.metadata.method.MethodDeclarationTree;
import edu.ucr.cs.riple.core.metadata.trackers.CompoundTracker;
import edu.ucr.cs.riple.core.metadata.trackers.RegionTracker;
import edu.ucr.cs.riple.core.util.Utility;

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

  protected final GlobalAnalyzer globalAnalyzer;

  /**
   * Constructor for target module supplier instance.
   *
   * @param config Annotator config instance.
   * @param globalAnalyzer Global analyzer instance.
   * @param tree Method declaration tree for methods in target module.
   */
  public TargetModuleSupplier(
      Config config, GlobalAnalyzer globalAnalyzer, MethodDeclarationTree tree) {
    super(ImmutableSet.of(config.target), config, tree);
    this.globalAnalyzer = globalAnalyzer;
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
  public GlobalAnalyzer getGlobalAnalyzer() {
    return globalAnalyzer;
  }

  @Override
  public ConflictGraphProcessor getImpactAnalyzer() {
    CompilerRunner runner = () -> Utility.buildTarget(config);
    if (config.optimized) {
      RegionTracker tracker = new CompoundTracker(config, config.target, tree);
      return new ParallelConflictGraphProcessor(config, runner, this, tracker);
    }
    return new SequentialConflictGraphProcessor(config, runner, this);
  }
}
