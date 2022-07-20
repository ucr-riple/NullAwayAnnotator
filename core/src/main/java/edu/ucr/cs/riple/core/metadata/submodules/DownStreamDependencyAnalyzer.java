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
import edu.ucr.cs.riple.core.metadata.method.MethodInheritanceTree;
import edu.ucr.cs.riple.core.metadata.method.MethodNode;
import edu.ucr.cs.riple.core.metadata.trackers.MethodRegionTracker;
import edu.ucr.cs.riple.core.util.Utility;
import java.util.Optional;

public class DownStreamDependencyAnalyzer {

  private final ImmutableSet<Module> modules;
  private final ImmutableSet<MethodStatus> methods;
  private final Config config;

  private final MethodInheritanceTree tree;

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
    this.methods =
        tree.getPublicMethodsWithNonPrimitivesReturn().stream()
            .map(MethodStatus::new)
            .collect(ImmutableSet.toImmutableSet());
  }

  public void explore() {
    modules.forEach(this::analyzeDownstreamDependency);
  }

  private void analyzeDownstreamDependency(Module module) {
    Utility.setScannerCheckerActivation(config, true);
    Utility.buildProject(config, module);
    Utility.setScannerCheckerActivation(config, false);
    MethodRegionTracker tracker = new MethodRegionTracker(config, tree);
  }

  public int effectOnDownstreamDependencies(String clazz, String method) {
    Optional<MethodStatus> optional =
        this.methods.stream()
            .filter(m -> m.node.method.equals(method) && m.node.clazz.equals(clazz))
            .findAny();
    return optional.map(methodStatus -> methodStatus.effect).orElse(0);
  }
}
