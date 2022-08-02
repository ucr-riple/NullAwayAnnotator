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

package edu.ucr.cs.riple.core.tools;

import static org.junit.Assert.fail;

import edu.ucr.cs.riple.core.Annotator;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.ModuleInfo;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.injector.Helper;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
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

  private final Set<Report> expectedReports;
  private final Path projectPath;
  private final Path srcSet;
  private final List<String> modules;
  private final Path outDirPath;
  private final Map<String, String[]> fileMap;
  private BiPredicate<Report, Report> predicate;
  private int depth = 1;
  private boolean requestCompleteLoop = false;
  private boolean disableBailout = false;
  private boolean downstreamDependencyAnalysisActivated = false;

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

  public CoreTestHelper setPredicate(BiPredicate<Report, Report> predicate) {
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

  public CoreTestHelper addExpectedReports(Report... reports) {
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

  public CoreTestHelper enableDownstreamDependencyAnalysis() {
    this.downstreamDependencyAnalysisActivated = true;
    return this;
  }

  public void start() {
    if (predicate == null) {
      predicate =
          (report, other) ->
              report.root.change.location.equals(other.root.change.location)
                  && report.effect == other.effect;
    }
    Path configPath = outDirPath.resolve("config.json");
    createFiles();
    checkSourcePackages();
    makeAnnotatorConfigFile(configPath);
    Config config = new Config(configPath.toString());
    Annotator annotator = new Annotator(config);
    annotator.start();
    compare(annotator.reports.values());
  }

  /** Checks if all src inputs are subpackages of test package. */
  private void checkSourcePackages() {
    try (Stream<Path> walk = Files.walk(srcSet)) {
      walk.filter(path -> path.toFile().isFile() && path.toFile().getName().endsWith(".java"))
          .forEach(
              path -> {
                if (!Helper.srcIsUnderClassClassPath(path, "test")) {
                  throw new IllegalArgumentException(
                      "Source files must have package declaration starting with \"test\": " + path);
                }
              });
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void compare(Collection<Report> actualOutput) {
    List<Report> notFound = new ArrayList<>();
    for (Report expected : expectedReports) {
      if (actualOutput.stream().noneMatch(report -> predicate.test(report, expected))) {
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
          .append(notFound.stream().map(Report::toString).collect(Collectors.toList()))
          .append("\n");
    }
    if (actualOutput.size() != 0) {
      errorMessage
          .append(actualOutput.size())
          .append(" unexpected Reports were found:")
          .append("\n")
          .append(actualOutput.stream().map(Report::toString).collect(Collectors.toList()))
          .append("\n");
    }
    fail(errorMessage.toString());
  }

  private void makeAnnotatorConfigFile(Path path) {
    Config.Builder builder = new Config.Builder();
    builder.buildCommand =
        Utility.computeBuildCommandWithGradleCLArguments(
            this.projectPath, this.outDirPath, modules);
    builder.configPaths =
        modules.stream()
            .map(
                name ->
                    new ModuleInfo(
                        outDirPath,
                        outDirPath.resolve(name + "-nullaway.xml"),
                        outDirPath.resolve(name + "-scanner.xml")))
            .collect(Collectors.toSet());
    builder.nullableAnnotation = "javax.annotation.Nullable";
    builder.initializerAnnotation = "test.Initializer";
    builder.outputDir = outDirPath.toString();
    builder.depth = depth;
    builder.bailout = !disableBailout;
    builder.chain = true;
    builder.outerLoopActivation = requestCompleteLoop;
    builder.optimized = true;
    builder.write(path);
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
}
