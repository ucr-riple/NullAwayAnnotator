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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utility {

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
   * from root to project root dir, command to compile the project and the computed paths to config
   * files which will be passed through gradle command line arguments.
   *
   * @param projectPath Path to project directory.
   * @param outDirPath Path to serialization output directory,
   * @param modules Set of names of the modules in the template.
   * @return The command to build the project including the command line arguments, this command can
   *     * be executed from any directory.
   */
  public static String computeBuildCommandWithGradleCLArguments(
      Path projectPath, Path outDirPath, List<String> modules) {
    return String.format(
        "%s && ./gradlew compileJava %s --rerun-tasks",
        Utility.changeDirCommand(projectPath),
        String.join(" ", computeConfigPathsWithGradleArguments(outDirPath, modules)));
  }
}
