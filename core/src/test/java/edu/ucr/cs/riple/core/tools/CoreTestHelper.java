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
import edu.ucr.cs.riple.core.Report;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CoreTestHelper {

  private final Set<Report> expectedReports;
  private final Path projectPath;
  private final Path srcSet;
  private final Path outDirPath;
  private final Map<String, String[]> fileMap;

  public CoreTestHelper(Path projectPath, Path outDirPath) {
    this.projectPath = projectPath;
    this.outDirPath = outDirPath;
    this.expectedReports = new HashSet<>();
    this.fileMap = new HashMap<>();
    this.srcSet = this.projectPath.resolve("src").resolve("main").resolve("java").resolve("test");
  }

  public CoreTestHelper addInputLines(String path, String... lines) {
    if (fileMap.containsKey(path)) {
      throw new IllegalArgumentException("File at path: " + path + " already exists.");
    }
    fileMap.put(path, lines);
    return this;
  }

  public CoreTestHelper addInputSourceFile(String path, String inputFilePath) {
    return addInputLines(path, Utility.readLinesOfFileFromResource(inputFilePath));
  }

  public CoreTestHelper addExpectedReports(Report... reports) {
    this.expectedReports.addAll(Arrays.asList(reports));
    return this;
  }

  public void start() {
    Path configPath = outDirPath.resolve("config.json");
    createFiles();
    makeAnnotatorConfigFile(configPath);
    Config config = new Config(configPath.toString());
    Annotator annotator = new Annotator(config);
    annotator.start();
    compare(annotator.reports.values());
  }

  private void compare(Collection<Report> actualOutput) {
    List<Report> notFound = new ArrayList<>();
    for (Report report : expectedReports) {
      if (actualOutput.stream().noneMatch(r -> r.equals(report) && r.effect == report.effect)) {
        notFound.add(report);
      } else {
        actualOutput.remove(report);
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
        Utility.changeDirCommand(projectPath.toString()) + " && ./gradlew compileJava";
    builder.cssConfigPath = outDirPath.resolve("css.xml").toString();
    builder.nullAwayConfigPath = outDirPath.resolve("config.xml").toString();
    builder.nullableAnnotation = "javax.annotation.Nullable";
    builder.initializerAnnotation = "annotator.test.Initializer";
    builder.outputDir = outDirPath.toString();
    builder.write(path);
  }

  private void createFiles() {
    fileMap.forEach(
        (key, value) -> {
          StringBuilder toWrite = new StringBuilder();
          for (String s : value) {
            toWrite.append(s).append("\n");
          }
          Utility.writeToFile(srcSet.resolve(key), toWrite.toString());
        });
  }
}
