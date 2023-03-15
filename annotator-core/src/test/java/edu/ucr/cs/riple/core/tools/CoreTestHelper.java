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

package edu.ucr.cs.riple.core.tools;

import static org.junit.Assert.fail;

import edu.ucr.cs.riple.core.AnalysisMode;
import edu.ucr.cs.riple.core.Annotator;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.ModuleInfo;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.injector.Helper;
import edu.ucr.cs.riple.scanner.generatedcode.SourceType;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;

/** Helper class for testing the core module. */
public class CoreTestHelper {

  /** The expected reports which gets compared with computed reports when process is finished. */
  private final Set<TReport> expectedReports;
  /** Path to the project. */
  private final Path projectPath;
  /** Path to source directory of target project. */
  private final Path srcSet;
  /**
   * List of modules to be analyzed. Module at index {@code 0} is the target module and all others
   * depend on it.
   */
  private final List<String> modules;
  /** Path to root directory where all output exists including the project. */
  private final Path outDirPath;
  /**
   * Predicate to be used for comparing reports. If not set by the user, {@link DEFAULT_PREDICATE}
   * will be used.
   */
  private BiPredicate<TReport, Report> predicate;
  /** Depth of the analysis. */
  private int depth = 1;
  /** Outer loop activation. Deactivated by default */
  private boolean outerLoopActivated = false;
  /** Bailout activation. Deactivated by default */
  private boolean disableBailout = false;
  /** Force resolve activation. Deactivated by default */
  private boolean suppressRemainingErrors = false;
  /** Downstream dependency analysis activation. Deactivated by default */
  private boolean downstreamDependencyAnalysisActivated = false;
  /** Inference activation. Activated by default */
  private boolean deactivateInference = false;
  /** Analysis mode. */
  private AnalysisMode mode = AnalysisMode.LOCAL;
  /** Annotator config. */
  private Config config;

  public CoreTestHelper(Path projectPath, Path outDirPath, List<String> modules) {
    this.projectPath = projectPath;
    this.outDirPath = outDirPath;
    this.expectedReports = new HashSet<>();
    this.srcSet = this.projectPath.resolve("src").resolve("main").resolve("java").resolve("test");
    this.modules = modules;
  }

  /**
   * Adds source code received as lines at the given output path.
   *
   * @param path Relative path to the file to be created from root source directory.
   * @param lines Lines of source code to be written.
   * @return This instance of {@link CoreTestHelper}.
   */
  public CoreTestHelper addInputLines(String path, String... lines) {
    try {
      FileUtils.writeLines(srcSet.resolve(path).toFile(), Arrays.asList(lines));
    } catch (IOException e) {
      throw new RuntimeException("Failed to write line at: " + path, e);
    }
    return this;
  }

  /**
   * Sets the predicate to be used for comparing reports. If not set by the user, {@link
   * DEFAULT_PREDICATE} will be used.
   *
   * @param predicate Predicate to be used for comparing reports.
   * @return This instance of {@link CoreTestHelper}.
   */
  public CoreTestHelper setPredicate(BiPredicate<TReport, Report> predicate) {
    this.predicate = predicate;
    return this;
  }

  /**
   * Adds source code which is read from resources at the given output path.
   *
   * @param path Relative path to the file to be created from root source directory.
   * @param inputFilePath Path to the file in resources.
   * @return This instance of {@link CoreTestHelper}.
   */
  public CoreTestHelper addInputSourceFile(String path, String inputFilePath) {
    try {
      return addInputLines(
          path,
          FileUtils.readFileToString(
              Utility.getPathOfResource(inputFilePath).toFile(), Charset.defaultCharset()));
    } catch (IOException e) {
      throw new RuntimeException("Failed to add source input", e);
    }
  }

  /**
   * Disables bailout.
   *
   * @return This instance of {@link CoreTestHelper}.
   */
  public CoreTestHelper disableBailOut() {
    disableBailout = true;
    return this;
  }

  /**
   * Adds source code which is read from a directory resources at the given output path.
   *
   * @param path Relative path to the directory to be created from root source directory.
   * @param inputDirectoryPath Path to the directory in resources.
   * @return This instance of {@link CoreTestHelper}.
   */
  public CoreTestHelper addInputDirectory(String path, String inputDirectoryPath) {
    Path dir = Utility.getPathOfResource(inputDirectoryPath);
    try {
      FileUtils.copyDirectory(dir.toFile(), srcSet.getParent().resolve(path).toFile());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Adds expected reports to be compared with computed reports when test process is finished.
   *
   * @param reports Expected Reports.
   * @return This instance of {@link CoreTestHelper}.
   */
  public CoreTestHelper addExpectedReports(TReport... reports) {
    this.expectedReports.addAll(Arrays.asList(reports));
    return this;
  }

  /**
   * Defines the depth of the analysis.
   *
   * @param depth Depth of the analysis.
   * @return This instance of {@link CoreTestHelper}.
   */
  public CoreTestHelper toDepth(int depth) {
    this.depth = depth;
    return this;
  }

  /**
   * Activates outer loop.
   *
   * @return This instance of {@link CoreTestHelper}.
   */
  public CoreTestHelper activateOuterLoop() {
    this.outerLoopActivated = true;
    return this;
  }

  /**
   * Activates downstream dependency analysis.
   *
   * @param mode Analysis mode.
   * @return This instance of {@link CoreTestHelper}.
   */
  public CoreTestHelper enableDownstreamDependencyAnalysis(AnalysisMode mode) {
    this.downstreamDependencyAnalysisActivated = true;
    if (mode.equals(AnalysisMode.LOCAL)) {
      throw new IllegalArgumentException(
          "Cannot perform downstream dependencies analysis with mode: " + mode.name());
    }
    this.mode = mode;
    return this;
  }

  /**
   * Suppresses remaining errors.
   *
   * @return This instance of {@link CoreTestHelper}.
   */
  public CoreTestHelper suppressRemainingErrors() {
    this.suppressRemainingErrors = true;
    return this;
  }

  public CoreTestHelper deactivateInference() {
    this.deactivateInference = true;
    this.suppressRemainingErrors = true;
    return this;
  }

  /**
   * Activates downstream dependency analysis with default mode.
   *
   * @return This instance of {@link CoreTestHelper}.
   */
  public CoreTestHelper enableDownstreamDependencyAnalysis() {
    return enableDownstreamDependencyAnalysis(AnalysisMode.LOWER_BOUND);
  }

  /** Starts the test process. */
  public void start() {
    Path configPath = outDirPath.resolve("config.json");
    checkSourcePackages();
    makeAnnotatorConfigFile(configPath);
    config = new Config(configPath);
    Annotator annotator = new Annotator(config);
    annotator.start();
    if (predicate == null) {
      predicate = DEFAULT_PREDICATE.create(config);
    }
    compare(new ArrayList<>(annotator.cache.reports()));
    checkBuildsStatus();
  }

  /** Checks if there is any build error including NullAway errors after the analysis. */
  private void checkBuildsStatus() {
    if (mode.equals(AnalysisMode.STRICT) && modules.size() > 1) {
      // Build downstream dependencies.
      Utility.executeCommand(config.downstreamDependenciesBuildCommand);
      // Verify no error is reported in downstream dependencies.
      for (int i = 1; i < modules.size(); i++) {
        Path path = outDirPath.resolve(i + "").resolve("errors.tsv");
        try {
          List<String> lines = Files.readAllLines(path);
          if (lines.size() != 1) {
            fail(
                "Strict mode introduced errors in downstream dependency module: "
                    + modules.get(i)
                    + ", errors:\n"
                    + lines);
          }
        } catch (IOException e) {
          throw new RuntimeException("Exception happened while reading file at: " + path);
        }
      }
    }
    if (suppressRemainingErrors) {
      // Check no error will be reported in Target module
      Utility.executeCommand(config.buildCommand);
      Path path = outDirPath.resolve("0").resolve("errors.tsv");
      try {
        List<String> lines = Files.readAllLines(path);
        if (lines.size() != 1) {
          fail("Force Resolve Mode did not resolve all errors:\n" + lines);
        }
      } catch (IOException e) {
        throw new RuntimeException("Exception happened while reading file at: " + path);
      }
    }
  }

  /** Checks if all src inputs are subpackages of test package. */
  private void checkSourcePackages() {
    try (Stream<Path> walk = Files.walk(projectPath)) {
      walk.filter(path -> path.toFile().isFile() && path.toFile().getName().endsWith(".java"))
          .forEach(
              path -> {
                if (!Helper.srcIsUnderClassClassPath(path, "test")) {
                  throw new IllegalArgumentException(
                      "Source files must have package declaration starting with \"test\": " + path);
                }
              });
    } catch (IOException e) {
      throw new RuntimeException("Error happened in processing src under: " + projectPath, e);
    }
  }

  /**
   * Compares expected reports with actual reports.
   *
   * @param actualOutput Actual reports.
   */
  private void compare(Collection<Report> actualOutput) {
    List<Report> notFound = new ArrayList<>();
    for (TReport expected : expectedReports) {
      if (actualOutput.stream().noneMatch(report -> predicate.test(expected, report))) {
        notFound.add(expected);
      } else {
        actualOutput.remove(expected);
      }
    }
    if (notFound.size() == 0 && actualOutput.size() == 0) {
      return;
    }
    StringBuilder errorMessage = new StringBuilder();
    if (notFound.size() != 0) {
      errorMessage
          .append(notFound.size())
          .append(" expected Reports were NOT found:")
          .append("\n")
          .append(notFound.stream().map(Report::toString).collect(Collectors.joining("\n")))
          .append("\n");
    }
    if (actualOutput.size() != 0) {
      errorMessage
          .append(actualOutput.size())
          .append(" unexpected Reports were found:")
          .append("\n")
          .append(actualOutput.stream().map(Report::toString).collect(Collectors.joining("\n")))
          .append("\n");
    }
    fail(errorMessage.toString());
  }

  /**
   * Creates a config file for annotator.
   *
   * @param configPath Path to the config file.
   */
  public void makeAnnotatorConfigFile(Path configPath) {
    Config.Builder builder = new Config.Builder();
    final int[] id = {0};
    builder.configPaths =
        modules.stream()
            .map(
                name ->
                    new ModuleInfo(
                        id[0]++,
                        outDirPath,
                        outDirPath.resolve(name + "-nullaway.xml"),
                        outDirPath.resolve(name + "-scanner.xml")))
            .collect(Collectors.toList());
    builder.nullableAnnotation = "javax.annotation.Nullable";
    builder.initializerAnnotation = "test.Initializer";
    builder.outputDir = outDirPath.toString();
    builder.depth = depth;
    builder.bailout = !disableBailout;
    builder.redirectBuildOutputToStdErr = true;
    builder.chain = true;
    builder.outerLoopActivation = outerLoopActivated;
    builder.useParallelProcessor = true;
    builder.downStreamDependenciesAnalysisActivated = downstreamDependencyAnalysisActivated;
    builder.mode = mode;
    builder.inferenceActivated = !deactivateInference;
    builder.forceResolveActivation = suppressRemainingErrors;
    builder.useCacheImpact = true;
    builder.sourceTypes.add(SourceType.LOMBOK);
    builder.cache = true;
    builder.useCacheImpact = !getEnvironmentVariable("ANNOTATOR_TEST_DISABLE_CACHING");
    builder.useParallelProcessor =
        !getEnvironmentVariable("ANNOTATOR_TEST_DISABLE_PARALLEL_PROCESSING");
    if (downstreamDependencyAnalysisActivated) {
      builder.buildCommand =
          Utility.computeBuildCommandWithLibraryModelLoaderDependency(
              this.projectPath, this.outDirPath, modules);
      builder.downstreamBuildCommand = builder.buildCommand;
      builder.nullawayLibraryModelLoaderPath =
          Utility.getPathToLibraryModel(outDirPath)
              .resolve(
                  Paths.get(
                      "src",
                      "main",
                      "resources",
                      "edu",
                      "ucr",
                      "cs",
                      "riple",
                      "librarymodel",
                      "nullable-methods.tsv"));
    } else {
      builder.buildCommand =
          Utility.computeBuildCommand(this.projectPath, this.outDirPath, modules);
    }
    builder.write(configPath);
  }

  /**
   * Gets the mode from environment variable. If the environment variable is not set, returns false.
   *
   * @param environmentVariableName environment variable name.
   * @return true if the environment variable is set and is true, false otherwise.
   */
  private boolean getEnvironmentVariable(String environmentVariableName) {
    String value = System.getenv(environmentVariableName);
    if (value == null || value.isEmpty()) {
      return false;
    }
    return Boolean.parseBoolean(value);
  }

  /**
   * Getter for config.
   *
   * @return Config instance.
   */
  public Config getConfig() {
    if (config == null) {
      throw new IllegalStateException(
          "Config has not been initialized yet, can only access it after a call of start method.");
    }
    return config;
  }

  /**
   * Returns path to src directory where all test inputs exist.
   *
   * @return Path to root src directory.
   */
  public Path getSourceRoot() {
    return getConfig()
        .globalDir
        .resolve("unittest")
        .resolve("src")
        .resolve("main")
        .resolve("java")
        .resolve("test");
  }

  /**
   * Default predicate for comparing expected and actual reports. The Default predicate only checks
   * if the expected effect is computed for the root fix along its tree.
   */
  static class DEFAULT_PREDICATE implements BiPredicate<TReport, Report> {

    /** The config. */
    private final Config config;

    private DEFAULT_PREDICATE(Config config) {
      this.config = config;
    }

    @Override
    public boolean test(TReport expected, Report found) {
      return expected.root.change.location.equals(found.root.change.location)
          && expected.getExpectedValue() == found.getOverallEffect(config);
    }

    /**
     * Creates a new instance of {@link DEFAULT_PREDICATE}.
     *
     * @param config the config
     * @return the default predicate
     */
    public static DEFAULT_PREDICATE create(Config config) {
      return new DEFAULT_PREDICATE(config);
    }
  }
}
