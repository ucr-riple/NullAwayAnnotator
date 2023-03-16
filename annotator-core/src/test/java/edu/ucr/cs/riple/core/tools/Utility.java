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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Utility class for tests. */
public class Utility {

  /**
   * Executes a shell command in a subprocess.
   *
   * @param command The shell command to run.
   */
  public static void executeCommand(String command) {
    try {
      Process p = Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", command});
      BufferedReader reader =
          new BufferedReader(new InputStreamReader(p.getErrorStream(), Charset.defaultCharset()));
      //noinspection StatementWithEmptyBody
      while (reader.readLine() != null) {}
      p.waitFor();
    } catch (Exception e) {
      throw new RuntimeException("Exception happened in executing command: " + command, e);
    }
  }

  /**
   * Gets the path of a resource from the given relative path.
   *
   * @param relativePath The relative path of the resource.
   * @return The path of the resource.
   */
  public static Path getPathOfResource(String relativePath) {
    return Paths.get(
        Objects.requireNonNull(Utility.class.getClassLoader().getResource(relativePath)).getFile());
  }

  /**
   * Creates the command to change directory to the given path.
   *
   * @param path The path to change directory to.
   * @return The command to change directory to the given path.
   */
  public static String changeDirCommand(Path path) {
    String os = System.getProperty("os.name").toLowerCase();
    return (os.startsWith("windows") ? "dir" : "cd") + " " + path;
  }

  /**
   * Creates the gradle command line arguments. Project names are used as a prefix for config files
   * names and output paths.
   *
   * @param outDirPath Root output path.
   * @param modules Name of all containing projects in the template. (Prefix for all variable name
   *     and values).
   * @return Gradle command line values with flags.
   */
  public static Set<String> computeConfigPathsWithGradleArguments(
      Path outDirPath, List<Module> modules) {
    return modules.stream()
        .flatMap(
            module -> {
              String nullawayConfigName = module + "-nullaway.xml";
              String scannerConfigName = module + "-scanner.xml";
              return Stream.of(
                  String.format(
                      "-P%s-nullaway-config-path=%s",
                      module, outDirPath.resolve(nullawayConfigName)),
                  String.format(
                      "-P%s-scanner-config-path=%s",
                      module, outDirPath.resolve(scannerConfigName)));
            })
        .collect(Collectors.toSet());
  }

  /**
   * Computes the path to NullAway library models loader. Library models loader resides in the unit
   * test temporary directory.
   *
   * @param unittestDir path to Unit test.
   * @return Path to nullaway library model loader.
   */
  public static Path getPathToLibraryModel(Path unittestDir) {
    return unittestDir.resolve("Annotator").resolve(Paths.get("library-model-loader"));
  }

  /**
   * Appends the given content to the given file.
   *
   * @param file The file to append to.
   * @param content The content to append.
   */
  public static void appendToFile(Path file, String content) {
    try {
      Files.writeString(file, content, Charset.defaultCharset(), StandardOpenOption.APPEND);
    } catch (IOException e) {
      throw new RuntimeException("Exception happened in appending to file: " + file, e);
    }
  }
}
