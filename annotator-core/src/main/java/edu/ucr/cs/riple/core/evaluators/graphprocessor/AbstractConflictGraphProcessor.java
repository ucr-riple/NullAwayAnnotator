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

package edu.ucr.cs.riple.core.evaluators.graphprocessor;

import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.evaluators.suppliers.Supplier;
import edu.ucr.cs.riple.core.global.GlobalModel;
import edu.ucr.cs.riple.core.injectors.AnnotationInjector;
import edu.ucr.cs.riple.core.metadata.graph.Node;
import edu.ucr.cs.riple.core.metadata.index.Bank;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.method.MethodDeclarationTree;
import edu.ucr.cs.riple.injector.changes.AddMarkerAnnotation;
import edu.ucr.cs.riple.injector.location.Location;
import java.util.Set;
import java.util.stream.Collectors;

/** Base class for conflict graph processors. */
public abstract class AbstractConflictGraphProcessor implements ConflictGraphProcessor {

  /** Method declaration tree. */
  protected final MethodDeclarationTree methodDeclarationTree;
  /** Injector used in the processor to inject / remove fixes. */
  protected final AnnotationInjector injector;
  /** Error bank instance to store state of fixes before and after of injections. */
  protected final Bank<Error> errorBank;
  /** Global model to retrieve impacts of fixes globally. */
  protected final GlobalModel globalModel;
  /** Annotator config. */
  protected final Config config;
  /** Handler to re-run compiler. */
  protected final CompilerRunner compilerRunner;

  public AbstractConflictGraphProcessor(Config config, CompilerRunner runner, Supplier supplier) {
    this.config = config;
    this.methodDeclarationTree = supplier.getMethodDeclarationTree();
    this.injector = supplier.getInjector();
    this.errorBank = supplier.getErrorBank();
    this.globalModel = supplier.getGlobalModel();
    this.compilerRunner = runner;
  }

  /**
   * Updates list of triggered fixes with fixes triggered from downstream dependencies.
   *
   * @param node Node in process.
   * @return Triggered fixes from downstream.
   */
  protected Set<Fix> getTriggeredFixesFromDownstream(Node node) {
    Set<Location> currentLocationTargetedByTree =
        node.tree.stream().map(Fix::toLocation).collect(Collectors.toSet());
    return globalModel.getTriggeredErrorsForCollection(node.tree).stream()
        .filter(
            error ->
                error.isSingleFix()
                    && error.toResolvingLocation().isOnParameter()
                    && error.isFixableOnTarget(methodDeclarationTree)
                    && !currentLocationTargetedByTree.contains(error.toResolvingLocation()))
        .map(
            error ->
                new Fix(
                    new AddMarkerAnnotation(
                        error.toResolvingLocation().toParameter(), config.nullableAnnot),
                    "PASSING_NULLABLE",
                    error.getRegion(),
                    false))
        .collect(Collectors.toSet());
  }
}
