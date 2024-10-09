/*
 * MIT License
 *
 * Copyright (c) 2020 Nima Karimipour
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

package edu.ucr.cs.riple.core.evaluators.graph.processors;

import edu.ucr.cs.riple.core.Context;
import edu.ucr.cs.riple.core.cache.downstream.DownstreamImpactCache;
import edu.ucr.cs.riple.core.evaluators.graph.Node;
import edu.ucr.cs.riple.core.evaluators.suppliers.Supplier;
import edu.ucr.cs.riple.core.injectors.AnnotationInjector;
import edu.ucr.cs.riple.core.module.ModuleInfo;
import edu.ucr.cs.riple.core.registries.index.Error;
import edu.ucr.cs.riple.core.registries.index.ErrorStore;
import edu.ucr.cs.riple.core.registries.index.Fix;
import java.util.Set;
import java.util.stream.Collectors;

/** Base class for conflict graph processors. */
public abstract class AbstractConflictGraphProcessor implements ConflictGraphProcessor {

  /** Injector used in the processor to inject / remove fixes. */
  protected final AnnotationInjector injector;

  /** Error store instance to store state of fixes before and after of injections. */
  protected final ErrorStore errorStore;

  /** Downstream impact cache to retrieve impacts of fixes globally. */
  protected final DownstreamImpactCache downstreamImpactCache;

  /** Annotator context. */
  protected final Context context;

  /** Handler to re-run compiler. */
  protected final CompilerRunner compilerRunner;

  /** ModuleInfo of the input module which the impact of fixes are computed on. */
  protected final ModuleInfo moduleInfo;

  public AbstractConflictGraphProcessor(Context context, CompilerRunner runner, Supplier supplier) {
    this.context = context;
    this.moduleInfo = supplier.getModuleInfo();
    this.injector = supplier.getInjector();
    this.downstreamImpactCache = supplier.getDownstreamImpactCache();
    this.errorStore = supplier.getErrorStore();
    this.compilerRunner = runner;
  }

  /**
   * Gets the set of triggered fixes on target module from downstream errors.
   *
   * @param node Node in process.
   */
  protected Set<Fix> getTriggeredFixesFromDownstreamErrors(Node node) {
    return downstreamImpactCache.getTriggeredErrorsForCollection(node.tree).stream()
        .filter(error -> error.isFixableOnTarget(context))
        .flatMap(Error::getResolvingFixesStream)
        .collect(Collectors.toSet());
  }
}
