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

import edu.ucr.cs.riple.core.Annotator;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.Report;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoreTestHelper {

  private final List<Report> expectedReports;
  private final List<Report> actualReports;
  private final Path projectPath;

  private final Path srcSet;
  private final Path outDirPath;
  private final Map<String, String[]> fileMap;
  private int depth;

  public CoreTestHelper(Path projectPath, Path outDirPath) {
    this.projectPath = projectPath;
    this.srcSet =
        projectPath
            .resolve("src")
            .resolve("main")
            .resolve("java")
            .resolve("annotator")
            .resolve("test");
    this.outDirPath = outDirPath;
    this.expectedReports = new ArrayList<>();
    this.fileMap = new HashMap<>();
    this.actualReports = new ArrayList<>();
  }

  public CoreTestHelper addInputLines(String path, String... input) {
    if (fileMap.containsKey(path)) {
      throw new IllegalArgumentException("File at path: " + path + " already exists.");
    }
    fileMap.put(path, input);
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
    readActualReports();
  }

  private void readActualReports() {}

  private void makeAnnotatorConfigFile(Path path) {
    Config.Builder builder = new Config.Builder();
    builder.buildCommand =
        Utility.changeDirCommand(projectPath.toString()) + " && ./gradlew compileJava";
    builder.cssConfigPath = Utility.changeDirCommand(outDirPath.resolve("css.xml").toString());
    builder.nullAwayConfigPath =
        Utility.changeDirCommand(outDirPath.resolve("config.xml").toString());
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
