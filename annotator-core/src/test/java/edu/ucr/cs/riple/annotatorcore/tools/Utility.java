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

package edu.ucr.cs.riple.annotatorcore.tools;

import edu.ucr.cs.riple.annotatorcore.Config;
import edu.ucr.cs.riple.annotatorcore.metadata.index.Error;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
   * Reads serialized errors at the given path.
   *
   * @param path Path to errors.tsv.
   * @return List of serialized errors.
   */
  public static List<Error> readErrorsFromOutputDirectory(Config config, Path path) {
    List<Error> errors = new ArrayList<>();
    try {
      try (BufferedReader br =
          Files.newBufferedReader(path.toFile().toPath(), Charset.defaultCharset())) {
        String line;
        // Skip headers.
        br.readLine();
        while ((line = br.readLine()) != null) {
          errors.add(config.getAdapter().deserializeError(line.split("\t")));
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Exception happened in reading errors at: " + path, e);
    }
    return errors;
  }

  public static Path getPathOfResource(String relativePath) {
    return Paths.get(
        Objects.requireNonNull(Utility.class.getClassLoader().getResource(relativePath)).getFile());
  }

  public static String changeDirCommand(Path path) {
    String os = System.getProperty("os.name").toLowerCase();
    return (os.startsWith("windows") ? "dir" : "cd") + " " + path;
  }

  public static ProcessBuilder createProcessInstance() {
    ProcessBuilder pb = new ProcessBuilder();
    String os = System.getProperty("os.name").toLowerCase();
    return os.startsWith("windows") ? pb.command("cmd.exe", "/c") : pb.command("bash", "-c");
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
      Path outDirPath, List<String> modules) {
    return modules.stream()
        .flatMap(
            name -> {
              String nullawayConfigName = name + "-nullaway.xml";
              String scannerConfigName = name + "-scanner.xml";
              return Stream.of(
                  String.format(
                      "-P%s-nullaway-config-path=%s", name, outDirPath.resolve(nullawayConfigName)),
                  String.format(
                      "-P%s-scanner-config-path=%s", name, outDirPath.resolve(scannerConfigName)));
            })
        .collect(Collectors.toSet());
  }

  /**
   * Computes the build command for the project template. It includes, changing directory command
   * from root to project root dir, command to compile the project, and the computed paths to config
   * files which will be passed through gradle command line arguments.
   *
   * @param projectPath Path to project directory.
   * @param outDirPath Path to serialization output directory,
   * @param modules Set of names of the modules in the template.
   * @return The command to build the project including the command line arguments, this command can
   *     * be executed from any directory.
   */
  public static String computeBuildCommand(
      Path projectPath, Path outDirPath, List<String> modules) {
    return String.format(
        "%s && ./gradlew compileJava %s -Plibrary-model-loader-path=%s --rerun-tasks",
        Utility.changeDirCommand(projectPath),
        String.join(" ", computeConfigPathsWithGradleArguments(outDirPath, modules)),
        getPathToLibraryModel(outDirPath).resolve(Paths.get("build", "libs", "librarymodel.jar")));
  }

  /**
   * Computes the build command for the project template. It includes, changing directory command
   * from root to project root dir, command to compile the project, command to update library model
   * loader jar and the computed paths to config files which will be passed through gradle command
   * line arguments.
   *
   * @param projectPath Path to project directory.
   * @param outDirPath Path to serialization output directory,
   * @param modules Set of names of the modules in the template.
   * @return The command to build the project including the command line arguments, this command can
   *     * be executed from any directory.
   */
  public static String computeBuildCommandWithLibraryModelLoaderDependency(
      Path projectPath, Path outDirPath, List<String> modules) {
    return String.format(
        "%s && ./gradlew library-model-loader:jar --rerun-tasks && %s && ./gradlew compileJava %s -Plibrary-model-loader-path=%s --rerun-tasks",
        Utility.changeDirCommand(outDirPath.resolve("Annotator")),
        Utility.changeDirCommand(projectPath),
        String.join(" ", computeConfigPathsWithGradleArguments(outDirPath, modules)),
        getPathToLibraryModel(outDirPath).resolve(Paths.get("build", "libs", "librarymodel.jar")));
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
}
