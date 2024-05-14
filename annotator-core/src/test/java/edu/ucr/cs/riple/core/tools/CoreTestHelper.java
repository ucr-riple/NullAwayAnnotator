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

import com.github.javaparser.ParserConfiguration;
import edu.ucr.cs.riple.core.AnalysisMode;
import edu.ucr.cs.riple.core.Annotator;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.checkers.nullaway.NullAway;
import edu.ucr.cs.riple.core.log.Log;
import edu.ucr.cs.riple.core.module.ModuleConfiguration;
import edu.ucr.cs.riple.injector.Helper;
import edu.ucr.cs.riple.scanner.generatedcode.SourceType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Helper class for testing the core module. */
public class CoreTestHelper {

  /** The expected reports which gets compared with computed reports when process is finished. */
  private final Set<TReport> expectedReports;
  /** Path to the project. */
  private final Path projectPath;
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
  /** Suppress remaining errors mode activation. Deactivated by default */
  private boolean suppressRemainingErrors = false;
  /** Downstream dependency analysis activation. Deactivated by default */
  private boolean downstreamDependencyAnalysisActivated = false;
  /** Inference activation. Activated by default */
  private boolean deactivateInference = false;
  /** Analysis mode. */
  private AnalysisMode mode = AnalysisMode.LOCAL;
  /** Annotator config. */
  private Config config;
  /**
   * Path to the expected output directory. If null, changes on source files will not be checked.
   */
  private Path expectedOutputPath;
  /** Project builder. */
  private final ProjectBuilder projectBuilder;
  /** Annotator log instance after the test execution. */
  private Log log;

  private ParserConfiguration.LanguageLevel languageLevel;

  public CoreTestHelper(Path projectPath, Path outDirPath) {
    this.projectPath = projectPath;
    this.outDirPath = outDirPath;
    this.expectedReports = new HashSet<>();
    this.projectBuilder = new ProjectBuilder(this, projectPath);
    this.languageLevel = ParserConfiguration.LanguageLevel.JAVA_11;
  }

  public Module onTarget() {
    return projectBuilder.onTarget();
  }

  /**
   * Runs the tests on an empty project. Sine all unit tests require a project to execute, this is
   * used to test features that do not require a project.
   */
  public CoreTestHelper onEmptyProject() {
    return projectBuilder.onEmptyProject().withExpectedReports();
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
   * Disables bailout.
   *
   * @return This instance of {@link CoreTestHelper}.
   */
  public CoreTestHelper disableBailOut() {
    disableBailout = true;
    return this;
  }

  /**
   * Adds expected reports to be compared with computed reports when test process is finished.
   *
   * @param reports Expected Reports.
   * @return This instance of {@link CoreTestHelper}.
   */
  CoreTestHelper addExpectedReports(TReport... reports) {
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

  /**
   * Checks if the changes on source files are as expected.
   *
   * @param expectedOutputPath Path to the expected output directory from resources. All files
   *     relative the root directory of the project and the given path will be compared.
   * @return This instance of {@link CoreTestHelper}.
   */
  public CoreTestHelper checkExpectedOutput(String expectedOutputPath) {
    this.expectedOutputPath = Utility.getPathOfResource(expectedOutputPath);
    return this;
  }

  public CoreTestHelper withLanguageLevel(ParserConfiguration.LanguageLevel languageLevel) {
    this.languageLevel = languageLevel;
    return this;
  }

  /** Starts the test process. */
  public void start() {
    Path configPath = outDirPath.resolve("config.json");
    checkSourcePackages();
    makeAnnotatorConfigFile(configPath);
    config = new Config(configPath);
    Annotator annotator = new Annotator(config);
    annotator.start();
    log = annotator.context.log;
    if (predicate == null) {
      predicate = DEFAULT_PREDICATE.create(config);
    }
    compare(new ArrayList<>(annotator.cache.reports()));
    checkBuildsStatus();
    checkExpectedOutput();
  }

  /** Checks if there is any build error including NullAway errors after the analysis. */
  private void checkBuildsStatus() {
    List<Module> modules = projectBuilder.getModules();
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
          fail("Suppress Remaining Errors Mode did not resolve all errors:\n" + lines);
        }
      } catch (IOException e) {
        throw new RuntimeException("Exception happened while reading file at: " + path);
      }
    }
  }

  /** Checks if the output is as expected. */
  private void checkExpectedOutput() {
    if (expectedOutputPath == null) {
      // Output will not be checked.
      return;
    }
    try (Stream<Path> walk =
        Files.walk(expectedOutputPath)
            .filter(path -> path.toFile().isFile() && path.toString().endsWith(".java"))) {
      walk.forEach(
          path -> {
            Path relativePath = expectedOutputPath.relativize(path);
            Path actualPath = projectPath.resolve(relativePath);
            if (!actualPath.toFile().exists()) {
              throw new RuntimeException("Expected output file does not exist: " + actualPath);
            }
            try {
              List<String> expectedLines = Files.readAllLines(path);
              List<String> actualLines = Files.readAllLines(actualPath);
              if (!expectedLines.equals(actualLines)) {
                String errorMessage =
                    "Output is not as expected, expected:"
                        + "\n"
                        + expectedLines.stream()
                            .map(Objects::toString)
                            .collect(Collectors.joining("\n"))
                        + "\n"
                        + "actual:"
                        + "\n"
                        + actualLines.stream()
                            .map(Objects::toString)
                            .collect(Collectors.joining("\n"));
                fail(errorMessage);
              }
            } catch (IOException e) {
              throw new RuntimeException("Exception happened while reading file at: " + path, e);
            }
          });
    } catch (IOException e) {
      throw new RuntimeException("Error happened in processing src under: " + projectPath, e);
    }
  }

  /** Checks if all src inputs are subpackages of test package. */
  private void checkSourcePackages() {
    try (Stream<Path> walk = Files.walk(projectPath)) {
      walk.filter(path -> path.toFile().isFile() && path.toFile().getName().endsWith(".java"))
          .forEach(
              path -> {
                if (!Helper.srcIsUnderClassClassPath(
                    path,
                    "test",
                    config != null
                        ? config.languageLevel
                        : ParserConfiguration.LanguageLevel.JAVA_11)) {
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
        projectBuilder.getModules().stream()
            .map(
                name ->
                    new ModuleConfiguration(
                        id[0]++,
                        outDirPath,
                        outDirPath.resolve(name + "-nullaway.xml"),
                        outDirPath.resolve(name + "-scanner.xml")))
            .collect(Collectors.toList());
    builder.checker = NullAway.NAME;
    builder.nullableAnnotation = "javax.annotation.Nullable";
    // In tests, we use NullAway @Initializer annotation.
    builder.initializerAnnotation = "com.uber.nullaway.annotations.Initializer";
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
    builder.suppressRemainingErrors = suppressRemainingErrors;
    builder.useCacheImpact = true;
    builder.sourceTypes.add(SourceType.LOMBOK);
    builder.cache = true;
    builder.languageLevel = languageLevel;
    builder.useCacheImpact = !getEnvironmentVariable("ANNOTATOR_TEST_DISABLE_CACHING");
    builder.useParallelProcessor =
        !getEnvironmentVariable("ANNOTATOR_TEST_DISABLE_PARALLEL_PROCESSING");
    if (downstreamDependencyAnalysisActivated) {
      builder.buildCommand =
          projectBuilder.computeTargetBuildCommandWithLibraryModelLoaderDependency(this.outDirPath);
      builder.downstreamBuildCommand =
          projectBuilder.computeDownstreamDependencyBuildCommandWithLibraryModelLoaderDependency(
              this.outDirPath);
      builder.nullawayLibraryModelLoaderPath =
          Utility.getPathToLibraryModel(outDirPath)
              .resolve(
                  Paths.get(
                      "src", "main", "resources", "edu", "ucr", "cs", "riple", "librarymodel"));
    } else {
      builder.buildCommand = projectBuilder.computeTargetBuildCommand(this.outDirPath);
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
   * Getter for Annotator's log after test execution.
   *
   * @return Log instance.
   */
  public Log getLog() {
    return log;
  }

  /**
   * Getter for Annotator's config instance which was executed on the test input.
   *
   * @return Config instance.
   */
  public Config getConfig() {
    return config;
  }

  /**
   * Default predicate for comparing expected and actual reports. The Default predicate only checks
   * if the expected effect is computed for the root fix along its tree.
   */
  static class DEFAULT_PREDICATE implements BiPredicate<TReport, Report> {

    /** Annotator config. */
    private final Config config;

    private DEFAULT_PREDICATE(Config config) {
      this.config = config;
    }

    @Override
    public boolean test(TReport expected, Report found) {
      return expected.root.change.getLocation().equals(found.root.change.getLocation())
          && expected.getExpectedValue() == found.getOverallEffect(config);
    }

    /**
     * Creates a new instance of {@link DEFAULT_PREDICATE}.
     *
     * @param config Annotator config.
     * @return the default predicate
     */
    public static DEFAULT_PREDICATE create(Config config) {
      return new DEFAULT_PREDICATE(config);
    }
  }
}
