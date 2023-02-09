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

package edu.ucr.cs.riple.scanner;

import edu.ucr.cs.riple.scanner.out.ClassInfo;
import edu.ucr.cs.riple.scanner.out.MethodInfo;
import edu.ucr.cs.riple.scanner.out.TrackerNode;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Serializer class where all generated files in Fix Serialization package is created through APIs
 * of this class.
 */
public class Serializer {

  /** Path to write field graph. */
  private final Path fieldGraphPath;
  /** Path to write impacted regions for changes on methods */
  private final Path methodImpactedRegion;
  /** Path to write method info metadata. */
  private final Path methodInfoPath;
  /** Path to write class info data. */
  private final Path classInfoPath;

  /** File name where all field usage data has been stored. */
  public static final String FIELD_GRAPH_FILE_NAME = "field_graph.tsv";
  /** File name where all impacted regions for changes on methods are serialized. */
  public static final String METHOD_IMPACTED_REGION_FILE_NAME = "impacted_region.tsv";
  /** File name where all method data has been stored. */
  public static final String METHOD_INFO_FILE_NAME = "method_info.tsv";
  /** File name where all class data has been stored. */
  public static final String CLASS_INFO_FILE_NAME = "class_info.tsv";

  public Serializer(Config config) {
    Path outputDirectory = config.getOutputDirectory();
    this.fieldGraphPath = outputDirectory.resolve(FIELD_GRAPH_FILE_NAME);
    this.methodImpactedRegion = outputDirectory.resolve(METHOD_IMPACTED_REGION_FILE_NAME);
    this.methodInfoPath = outputDirectory.resolve(METHOD_INFO_FILE_NAME);
    this.classInfoPath = outputDirectory.resolve(CLASS_INFO_FILE_NAME);
    initializeOutputFiles(config);
  }

  /**
   * Appends the string representation of the {@link TrackerNode} corresponding to method impacted
   * region.
   *
   * @param methodImpactedRegion TrackerNode instance which is an impacted region for a method
   *     change.
   */
  public void serializeImpactedRegionByMethod(TrackerNode methodImpactedRegion) {
    appendToFile(methodImpactedRegion.toString(), this.methodImpactedRegion);
  }

  /**
   * Appends the string representation of the {@link TrackerNode} corresponding to a field graph.
   *
   * @param fieldGraphNode TrackerNode instance.
   */
  public void serializeFieldGraphNode(TrackerNode fieldGraphNode) {
    appendToFile(fieldGraphNode.toString(), this.fieldGraphPath);
  }

  /**
   * Appends the string representation of the {@link ClassInfo} corresponding to a compilation unit
   * tree.
   *
   * @param classInfo ClassInfo instance.
   */
  public void serializeClassInfo(ClassInfo classInfo) {
    appendToFile(classInfo.toString(), this.classInfoPath);
  }

  /**
   * Appends the string representation of the {@link MethodInfo} corresponding to a method.
   *
   * @param methodInfo MethodInfo instance.
   */
  public void serializeMethodInfo(MethodInfo methodInfo) {
    appendToFile(methodInfo.toString(), this.methodInfoPath);
  }

  /** Cleared the content of the file if exists and writes the header in the first line. */
  private void initializeFile(Path path, String header) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException e) {
      throw new RuntimeException("Could not clear file at: " + path, e);
    }
    try (OutputStream os = new FileOutputStream(path.toFile())) {
      header += "\n";
      os.write(header.getBytes(Charset.defaultCharset()), 0, header.length());
      os.flush();
    } catch (IOException e) {
      throw new RuntimeException("Could not finish resetting File at Path: " + path, e);
    }
  }

  /** Initializes every file which will be re-generated in the new run of NullAway. */
  private void initializeOutputFiles(Config config) {
    try {
      Files.createDirectories(config.getOutputDirectory());
      if (config.callTrackerIsActive()) {
        initializeFile(methodImpactedRegion, TrackerNode.header());
      }
      if (config.fieldTrackerIsActive()) {
        initializeFile(fieldGraphPath, TrackerNode.header());
      }
      if (config.methodTrackerIsActive()) {
        initializeFile(methodInfoPath, MethodInfo.header());
      }
      if (config.classTrackerIsActive()) {
        initializeFile(classInfoPath, ClassInfo.header());
      }
    } catch (IOException e) {
      throw new RuntimeException("Could not finish resetting serializer", e);
    }
  }

  /**
   * Appends the given string as a row in the file which tha path is given.
   *
   * @param row Row to append.
   * @param path Path to target file.
   */
  private void appendToFile(String row, Path path) {
    // Since there is no method available in API of either javac or errorprone to inform NullAway
    // that the analysis is finished, we cannot open a single stream and flush it within a finalize
    // method. Must open and close a new stream everytime we are appending a new line to a file.
    if (row == null || row.equals("")) {
      return;
    }
    row = row + "\n";
    try (OutputStream os = new FileOutputStream(path.toFile(), true)) {
      os.write(row.getBytes(Charset.defaultCharset()), 0, row.length());
      os.flush();
    } catch (IOException e) {
      throw new RuntimeException("Error happened for writing at file: " + path, e);
    }
  }
}
