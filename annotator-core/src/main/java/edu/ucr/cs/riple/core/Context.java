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

package edu.ucr.cs.riple.core;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.checkers.Checker;
import edu.ucr.cs.riple.core.checkers.CheckerBaseClass;
import edu.ucr.cs.riple.core.injectors.AnnotationInjector;
import edu.ucr.cs.riple.core.injectors.PhysicalInjector;
import edu.ucr.cs.riple.core.log.Log;
import edu.ucr.cs.riple.core.module.ModuleConfiguration;
import edu.ucr.cs.riple.core.module.ModuleInfo;
import edu.ucr.cs.riple.core.registries.index.Error;
import edu.ucr.cs.riple.injector.offsets.FileOffsetStore;
import edu.ucr.cs.riple.injector.offsets.OffsetChange;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Context class for Annotator. This class encapsulates all the code structure information all APIs
 * might need to access. Each API method knows what it needs to access and can access it from the
 * context.
 */
public class Context {

  /** Annotator configuration. */
  public final Config config;

  /** Log instance. Responsible for logging all the information about the build time and count. */
  public final Log log;

  /** Handler for computing the original offset of reported errors with existing changes. */
  public final OffsetHandler offsetHandler;

  /** The moduleInfo of target module. */
  public final ModuleInfo targetModuleInfo;

  /**
   * Configuration of the target module, required for building the target module and collecting
   * checkers output.
   */
  public final ModuleConfiguration targetConfiguration;

  /** Sets of context path information for all downstream dependencies. */
  public final ImmutableSet<ModuleConfiguration> downstreamConfigurations;

  /** Checker instance. Used to execute checker specific tasks. */
  public final Checker<? extends Error> checker;

  public final AnnotationInjector injector;

  /**
   * Builds context from command line arguments.
   *
   * @param config Annotator config.
   */
  public Context(Config config) {
    this.config = config;
    this.offsetHandler = new OffsetHandler();
    this.downstreamConfigurations = config.downstreamConfigurations;
    this.log = new Log();
    this.targetConfiguration = config.target;
    this.checker = CheckerBaseClass.getCheckerByName(config.checkerName, this);
    this.targetModuleInfo = new ModuleInfo(this, config.target, config.buildCommand);
    // Checker compatibility check must be after target module info is initialized.
    this.checker.verifyCheckerCompatibility();
    this.injector = new PhysicalInjector(this);
  }

  /**
   * Gets the injector.
   *
   * @return Injector instance.
   */
  public AnnotationInjector getInjector() {
    return injector;
  }

  /** Responsible for handling offset changes in source file. */
  public static class OffsetHandler {

    /** Map of file paths to Offset stores. */
    private final Map<Path, FileOffsetStore> contents;

    public OffsetHandler() {
      this.contents = new HashMap<>();
    }

    /**
     * Gets the original offset according to existing offset changes.
     *
     * @param path Path to source file.
     * @param offset Given offset.
     * @return Original offset.
     */
    public int getOriginalOffset(Path path, int offset) {
      if (!contents.containsKey(path)) {
        return offset;
      }
      return OffsetChange.getOriginalOffset(offset, contents.get(path).getOffsetChanges());
    }

    /**
     * Updates given offsets with given new offset changes.
     *
     * @param newOffsets Given new offset changes.
     */
    public void updateStateWithRecentChanges(Set<FileOffsetStore> newOffsets) {
      newOffsets.forEach(
          store -> {
            if (!contents.containsKey(store.getPath())) {
              contents.put(store.getPath(), store);
            } else {
              contents
                  .get(store.getPath())
                  .updateStateWithNewOffsetChanges(store.getOffsetChanges());
            }
          });
    }
  }
}
