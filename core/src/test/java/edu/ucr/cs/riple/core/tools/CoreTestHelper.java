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

import com.uber.nullaway.NullAway;
import edu.ucr.cs.riple.core.Report;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class CoreTestHelper {

  private final List<Report> reports;
  private final Path rootPath;
  private final Map<String, String[]> fileMap;
  private final int depth;

  public CoreTestHelper(Path path, int depth) {
    this.rootPath = path;
    this.reports = new ArrayList<>();
    this.fileMap = new HashMap<>();
    this.depth = depth;
    System.out.println("Working Directory = " + System.getProperty("user.dir"));
  }

  public CoreTestHelper addInputLines(String path, String... input) {
    if (fileMap.containsKey(path)) {
      throw new IllegalArgumentException("File at path: " + path + " already exists.");
    }
    fileMap.put(path, input);
    return this;
  }

  public CoreTestHelper addInputSourceFile(String path, String inputFilePath) {
    String[] lines = readLinesOfFileFromResources(inputFilePath);
    return addInputLines(path, lines);
  }

  public CoreTestHelper addExpectedReports(Report... reports) {
    this.reports.addAll(Arrays.asList(reports));
    return this;
  }

  public void start() {
    Path outputPath = rootPath.resolve("fixes.tsv");
    try {
      Files.deleteIfExists(outputPath);
    } catch (IOException ignored) {
      throw new RuntimeException("Failed to delete older file at: " + outputPath);
    }
  }

  private String[] readLinesOfFileFromResources(String path) {
    BufferedReader reader;
    List<String> lines = new ArrayList<>();
    try {
      reader =
          new BufferedReader(
              new FileReader(
                  Objects.requireNonNull(getClass().getClassLoader().getResource(path)).getFile()));
      String line = reader.readLine();
      while (line != null) {
        lines.add(line);
        line = reader.readLine();
      }
      reader.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return lines.toArray(new String[0]);
  }

  private void makeDirectories() {
    String[] names = {"src"};
    for (String name : names) {
      String pathToDirectory = rootPath + "/" + name;
      try {
        Files.createDirectories(Paths.get(pathToDirectory + "/"));
      } catch (IOException e) {
        throw new RuntimeException("Could not create the directories for name: " + name);
      }
    }
  }

  void writeToFile(String relativePath, String[] input) {
    StringBuilder toWrite = new StringBuilder();
    for (String s : input) {
      toWrite.append(s).append("\n");
    }
    writeToFile(relativePath, toWrite.toString());
  }

  void writeToFile(String relativePath, String input) {
    input = input.replace("\\", "");
    relativePath = rootPath.resolve(relativePath).toAbsolutePath().toString();
    String pathToFileDirectory = relativePath.substring(0, relativePath.lastIndexOf("/"));
    try {
      Files.createDirectories(Paths.get(pathToFileDirectory + "/"));
      try (Writer writer =
          Files.newBufferedWriter(Paths.get(relativePath), Charset.defaultCharset())) {
        writer.write(input);
        writer.flush();
      }
    } catch (IOException e) {
      throw new RuntimeException("Something terrible happened.", e);
    }
  }

  private String readFileToString(String path) {
    StringBuilder contentBuilder = new StringBuilder();
    try (Stream<String> stream = Files.lines(Paths.get(path), Charset.defaultCharset())) {
      stream.forEach(s -> contentBuilder.append(s).append("\n"));
      return contentBuilder.toString();
    } catch (FileNotFoundException ex) {
      throw new RuntimeException("Unable to open file: " + path, ex);
    } catch (IOException ex) {
      throw new RuntimeException("Error reading file: " + path, ex);
    }
  }
}
