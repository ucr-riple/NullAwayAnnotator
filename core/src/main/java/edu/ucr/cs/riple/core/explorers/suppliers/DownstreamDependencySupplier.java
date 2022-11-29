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

import edu.ucr.cs.riple.core.CompilerRunner;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.explorers.impactanalyzers.BasicImpactAnalyzer;
import edu.ucr.cs.riple.core.explorers.impactanalyzers.ImpactAnalyzer;
import edu.ucr.cs.riple.core.explorers.impactanalyzers.OptimizedImpactAnalyzer;
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
  public CompilerRunner getCompilerRunner() {
    return () -> Utility.buildDownstreamDependencies(config);
  }

  @Override
  public ImpactAnalyzer getImpactAnalyzer() {
    return config.optimized
        ? new OptimizedImpactAnalyzer(config, this, tracker)
        : new BasicImpactAnalyzer(config, this);
  }
}
