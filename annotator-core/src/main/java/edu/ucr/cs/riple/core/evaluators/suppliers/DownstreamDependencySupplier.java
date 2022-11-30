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

import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.evaluators.graphprocessor.AbstractConflictGraphProcessor;
import edu.ucr.cs.riple.core.evaluators.graphprocessor.CompilerRunner;
import edu.ucr.cs.riple.core.evaluators.graphprocessor.ParallelConflictGraphProcessor;
import edu.ucr.cs.riple.core.evaluators.graphprocessor.SequentialConflictGraphProcessor;
import edu.ucr.cs.riple.core.global.GlobalAnalyzer;
import edu.ucr.cs.riple.core.global.NoOpGlobalAnalyzer;
import edu.ucr.cs.riple.core.injectors.AnnotationInjector;
import edu.ucr.cs.riple.core.injectors.VirtualInjector;
import edu.ucr.cs.riple.core.metadata.method.MethodDeclarationTree;
import edu.ucr.cs.riple.core.metadata.trackers.RegionTracker;
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
  public GlobalAnalyzer getGlobalAnalyzer() {
    return new NoOpGlobalAnalyzer();
  }

  @Override
  public AbstractConflictGraphProcessor getImpactAnalyzer() {
    CompilerRunner runner = () -> Utility.buildDownstreamDependencies(config);
    return config.optimized
        ? new ParallelConflictGraphProcessor(config, runner, this, tracker)
        : new SequentialConflictGraphProcessor(config, runner, this);
  }
}
