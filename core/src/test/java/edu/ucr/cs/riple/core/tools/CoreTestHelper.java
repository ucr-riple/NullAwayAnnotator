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
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.injector.Helper;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;

public class CoreTestHelper {

  private final Set<TReport> expectedReports;
  private final Path projectPath;
  private final Path srcSet;
  private final List<String> modules;
  private final Path outDirPath;
  private final Map<String, String[]> fileMap;
  private BiPredicate<TReport, Report> predicate;
  private int depth = 1;
  private boolean requestCompleteLoop = false;
  private boolean disableBailout = false;
  private boolean forceResolveActivated = false;
  private boolean downstreamDependencyAnalysisActivated = false;
  private AnalysisMode mode = AnalysisMode.LOCAL;
  private Config config;
  private boolean chain = true;

  public CoreTestHelper(Path projectPath, Path outDirPath, List<String> modules) {
    this.projectPath = projectPath;
    this.outDirPath = outDirPath;
    this.expectedReports = new HashSet<>();
    this.fileMap = new HashMap<>();
    this.srcSet = this.projectPath.resolve("src").resolve("main").resolve("java").resolve("test");
    this.modules = modules;
  }

  public CoreTestHelper addInputLines(String path, String... lines) {
    if (fileMap.containsKey(path)) {
      throw new IllegalArgumentException("File at path: " + path + " already exists.");
    }
    fileMap.put(path, lines);
    return this;
  }

  public CoreTestHelper setPredicate(BiPredicate<TReport, Report> predicate) {
    this.predicate = predicate;
    return this;
  }

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

  public CoreTestHelper disableBailOut() {
    disableBailout = true;
    return this;
  }

  public CoreTestHelper addInputDirectory(String path, String inputDirectoryPath) {
    Path dir = Utility.getPathOfResource(inputDirectoryPath);
    try {
      FileUtils.copyDirectory(dir.toFile(), srcSet.getParent().resolve(path).toFile());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  public CoreTestHelper addExpectedReports(TReport... reports) {
    this.expectedReports.addAll(Arrays.asList(reports));
    return this;
  }

  public CoreTestHelper toDepth(int depth) {
    this.depth = depth;
    return this;
  }

  public CoreTestHelper requestCompleteLoop() {
    this.requestCompleteLoop = true;
    return this;
  }

  public CoreTestHelper enableDownstreamDependencyAnalysis(AnalysisMode mode) {
    this.downstreamDependencyAnalysisActivated = true;
    if (mode.equals(AnalysisMode.LOCAL)) {
      throw new IllegalArgumentException(
          "Cannot perform downstream dependencies analysis with mode: " + mode.name());
    }
    this.mode = mode;
    return this;
  }

  public CoreTestHelper enableForceResolve() {
    this.forceResolveActivated = true;
    return this;
  }

  public CoreTestHelper enableDownstreamDependencyAnalysis() {
    return enableDownstreamDependencyAnalysis(AnalysisMode.LOWER_BOUND);
  }

  public CoreTestHelper applyRootFixOnly() {
    this.chain = false;
    return this;
  }

  public void start() {
    Path configPath = outDirPath.resolve("config.json");
    createFiles();
    checkSourcePackages();
    makeAnnotatorConfigFile(configPath);
    config = new Config(configPath);
    Annotator annotator = new Annotator(config);
    annotator.start();
    if (predicate == null) {
      predicate =
          (expected, found) ->
              expected.root.change.location.equals(found.root.change.location)
                  && expected.getExpectedValue() == found.getOverallEffect(config);
    }
    compare(new ArrayList<>(annotator.cache.reports()));
    checkBuildsStatus();
  }

  private void checkBuildsStatus() {
    if (mode.equals(AnalysisMode.STRICT) && modules.size() > 1) {
      // Build downstream dependencies.
      Utility.executeCommand(config.downstreamDependenciesBuildCommand);
      // Verify no error is reported in downstream dependencies.
      for (int i = 1; i < modules.size(); i++) {
        Path path = outDirPath.resolve(i + "").resolve("errors.tsv");
        List<Error> errors = Utility.readErrorsFromOutputDirectory(path);
        if (errors.size() != 0) {
          fail(
              "Strict mode introduced errors in downstream dependency module: "
                  + modules.get(i)
                  + ", errors:\n"
                  + errors.stream().map(Error::toString).collect(Collectors.joining("\n")));
        }
      }
    }
    if (forceResolveActivated) {
      // Check no error will be reported in Target module
      Utility.executeCommand(config.buildCommand);
      Path path = outDirPath.resolve("0").resolve("errors.tsv");
      List<Error> errors = Utility.readErrorsFromOutputDirectory(path);
      if (errors.size() != 0) {
        fail(
            "Force Resolve Mode did not resolve all errors:\n"
                + errors.stream().map(Error::toString).collect(Collectors.joining("\n")));
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

  private void makeAnnotatorConfigFile(Path configPath) {
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
    builder.chain = chain;
    builder.outerLoopActivation = requestCompleteLoop;
    builder.optimized = true;
    builder.downStreamDependenciesAnalysisActivated = downstreamDependencyAnalysisActivated;
    builder.mode = mode;
    builder.forceResolveActivation = forceResolveActivated;
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

  private void createFiles() {
    fileMap.forEach(
        (key, value) -> {
          try {
            FileUtils.writeLines(srcSet.resolve(key).toFile(), Arrays.asList(value));
          } catch (IOException e) {
            throw new RuntimeException("Failed to write line at: " + key, e);
          }
        });
  }

  public Config getConfig() {
    if (config == null) {
      throw new IllegalStateException(
          "Config has not been initialized yet, can only access it after a call of start method.");
    }
    return config;
  }
}
