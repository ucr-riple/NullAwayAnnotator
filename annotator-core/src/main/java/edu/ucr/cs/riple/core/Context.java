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
import edu.ucr.cs.riple.core.io.deserializers.CheckerDeserializer;
import edu.ucr.cs.riple.core.log.Log;
import edu.ucr.cs.riple.core.module.ModuleConfiguration;
import edu.ucr.cs.riple.core.module.ModuleInfo;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.offsets.FileOffsetStore;
import edu.ucr.cs.riple.injector.offsets.OffsetChange;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
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
  /** Deserializer for reading the output of the checker. */
  public final CheckerDeserializer deserializer;
  /** Checker enum to retrieve checker specific instances. (e.g. {@link CheckerDeserializer}) */
  public final Checker checker;

  /**
   * Builds context from command line arguments.
   *
   * @param config Annotator config.
   */
  public Context(Config config) {
    this.config = config;
    this.checker = config.checker;
    this.offsetHandler = new OffsetHandler();
    this.downstreamConfigurations = config.downstreamConfigurations;
    this.log = new Log();
    this.targetConfiguration = config.target;
    this.deserializer = initializeCheckerDeserializer();
    this.targetModuleInfo = new ModuleInfo(this, config.target, config.buildCommand);
  }

  /**
   * Initializes the checker deserializer based on the checker name and the serialization version.
   *
   * @return the checker deserializer associated with the requested checker name and version.
   */
  public CheckerDeserializer initializeCheckerDeserializer() {
    // To retrieve the serialization version, we need to build the target first.
    Utility.buildTarget(this);
    Path serializationVersionPath = config.target.dir.resolve("serialization_version.txt");
    if (!serializationVersionPath.toFile().exists()) {
      // Older versions of checkers
      throw new RuntimeException(
          "Serialization version not found. Upgrade to newer versions of the checkers: "
              + checker.name());
    }
    List<String> lines = Utility.readFileLines(serializationVersionPath);
    int version = Integer.parseInt(lines.get(0));
    CheckerDeserializer deserializer = checker.getDeserializer(this);
    if (deserializer.getVersionNumber() == version) {
      return deserializer;
    } else {
      throw new RuntimeException(
          "Serialization version mismatch. Upgrade new versions of checkers.");
    }
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
