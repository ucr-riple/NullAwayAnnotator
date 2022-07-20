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

package edu.ucr.cs.riple.core.metadata.submodules;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.explorers.Explorer;
import edu.ucr.cs.riple.core.explorers.OptimizedExplorer;
import edu.ucr.cs.riple.core.injectors.AnnotationInjector;
import edu.ucr.cs.riple.core.injectors.VirtualInjector;
import edu.ucr.cs.riple.core.metadata.field.FieldDeclarationAnalysis;
import edu.ucr.cs.riple.core.metadata.index.Bank;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.method.MethodInheritanceTree;
import edu.ucr.cs.riple.core.metadata.method.MethodNode;
import edu.ucr.cs.riple.core.metadata.trackers.MethodRegionTracker;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.changes.AddAnnotation;
import edu.ucr.cs.riple.injector.location.OnMethod;
import java.util.Optional;
import java.util.stream.Stream;

public class DownStreamDependencyAnalyzer {

  private final ImmutableSet<Module> modules;
  private final Stream<MethodStatus> methods;
  private final Config config;
  private final MethodInheritanceTree tree;
  private final AnnotationInjector injector;

  static class MethodStatus {
    final MethodNode node;
    int effect;

    public MethodStatus(MethodNode node) {
      this.node = node;
      this.effect = 0;
    }
  }

  public DownStreamDependencyAnalyzer(Config config, MethodInheritanceTree tree) {
    this.config = config;
    this.modules = config.getSubModules();
    this.tree = tree;
    this.injector = new VirtualInjector(config);
    this.methods = tree.getPublicMethodsWithNonPrimitivesReturn().stream().map(MethodStatus::new);
  }

  public void explore() {
    modules.forEach(this::analyzeDownstreamDependency);
  }

  private void analyzeDownstreamDependency(Module module) {
    Utility.setScannerCheckerActivation(config, true);
    Utility.buildProject(config, module);
    Utility.setScannerCheckerActivation(config, false);
    MethodRegionTracker tracker = new MethodRegionTracker(config, tree);
    ImmutableSet<Fix> fixes =
        methods
            .filter(
                input -> !tracker.getCallersOfMethod(input.node.clazz, input.node.method).isEmpty())
            .map(
                methodStatus ->
                    new Fix(
                        new AddAnnotation(
                            new OnMethod("null", methodStatus.node.clazz, methodStatus.node.method),
                            config.nullableAnnot),
                        "null",
                        "null",
                        "null"))
            .collect(ImmutableSet.toImmutableSet());
    FieldDeclarationAnalysis fieldDeclarationAnalysis =
        new FieldDeclarationAnalysis(config.dir.resolve("class_info.tsv"));
    Bank<Error> errorBank = new Bank<>(config.dir.resolve("errors.tsv"), Error::new);
    Bank<Fix> fixBank =
        new Bank<>(config.dir.resolve("fixes.tsv"), Fix.factory(config, fieldDeclarationAnalysis));
    Explorer explorer =
        new OptimizedExplorer(injector, errorBank, fixBank, tracker, fixes, tree, 1, config);
    ImmutableSet<Report> reports = explorer.explore();
    methods.forEach(
        method -> {
          MethodNode node = method.node;
          Optional<Report> optional =
              reports.stream()
                  .filter(
                      input ->
                          input.root.toMethod().method.equals(node.method)
                              && input.root.toMethod().clazz.equals(node.clazz))
                  .findAny();
          optional.ifPresent(report -> method.effect += report.effect);
        });
  }

  public int effectOnDownstreamDependencies(Fix fix) {
    if (!fix.isOnMethod()) {
      return 0;
    }
    OnMethod onMethod = fix.toMethod();
    Optional<MethodStatus> optional =
        this.methods
            .filter(
                m -> m.node.method.equals(onMethod.method) && m.node.clazz.equals(onMethod.clazz))
            .findAny();
    return optional.map(methodStatus -> methodStatus.effect).orElse(0);
  }
}
