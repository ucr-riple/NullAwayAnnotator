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

package edu.ucr.cs.riple.core;

import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import edu.ucr.cs.riple.core.registries.index.Fix;
import edu.ucr.cs.riple.core.util.GitUtility;
import edu.ucr.cs.riple.injector.location.OnField;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/** Starting point. */
public class Main {

  static class Benchmark {
    public final String annotatedPackage;
    public final String path;
    public final String buildCommand;

    public Benchmark(String annotatedPackage, String path, String buildCommand) {
      this.annotatedPackage = annotatedPackage;
      this.path = path;
      this.buildCommand = buildCommand;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Benchmark)) {
        return false;
      }
      Benchmark benchmark = (Benchmark) o;
      return Objects.equals(annotatedPackage, benchmark.annotatedPackage)
          && Objects.equals(path, benchmark.path)
          && Objects.equals(buildCommand, benchmark.buildCommand);
    }

    @Override
    public int hashCode() {
      return Objects.hash(annotatedPackage, path, buildCommand);
    }
  }

  // Benchmarks
  static final Map<String, Benchmark> benchmarks;

  static {
    benchmarks = Map.of("libgdx", new Benchmark("com.badlogic.gdx", "libgdx", "compileJava"));
  }

  // PROJECT SPECIFIC CONFIGURATION
  // Ubuntu
  public static final boolean DEBUG_MODE = false;
  public static final String DEBUG_LINE = "return (Class<T>) PRIMITIVE_TYPES.get(clazz);";

  // COMMON CONFIGURATION
  public static final Path LOG_PATH = Paths.get("/tmp/logs/app.log");
  public static final Path COMMIT_HASH_PATH = Paths.get("/tmp/logs/commits.tsv");
  public static final Path TIMER_PATH = Paths.get("/tmp/logs/timer.txt");

  public static void main(String[] args) {
    System.clearProperty("ANNOTATOR_TEST_MODE");
    String benchmarkName = args[0];
    boolean isBaseline = false;
    if (args.length > 1) {
      isBaseline = args[1].equals("basic");
    }
    System.out.println("Running " + benchmarkName);
    // DELETE LOG:
    try {
      MoreFiles.deleteRecursively(LOG_PATH.getParent(), RecursiveDeleteOption.ALLOW_INSECURE);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    Benchmark benchmark = benchmarks.get(benchmarkName);
    if (benchmark == null) {
      throw new IllegalArgumentException("Unknown benchmark: " + benchmarkName);
    }
    String PROJECT_PATH = "/home/nima/Developer/nullness-benchmarks/" + benchmark.path;

    // delete dir
    Path outDir = Paths.get(PROJECT_PATH + "/annotator-out/0");

    if (outDir.toFile().exists()) {
      try {
        Files.walkFileTree(
            outDir,
            new SimpleFileVisitor<>() {
              @Override
              public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                  throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
              }

              @Override
              public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                  throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
              }
            });
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    String[] argsArray = {
      "-d",
      String.format("%s/annotator-out", PROJECT_PATH),
      "-bc",
      // For Ubuntu
      String.format(
          "export JAVA_HOME=/usr/lib/jvm/java-1.17.0-openjdk-amd64 && cd %s && ANDROID_HOME=/home/nima/Android/Sdk ./gradlew clean %s --rerun-tasks --no-build-cache",
          PROJECT_PATH, benchmark.buildCommand),
      //      // For Mac
      //      String.format(
      //          "JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.0.2.jdk/Contents/Home &&cd %s
      // && ./gradlew clean conductor-core:compileJava --rerun-tasks --no-build-cache",
      //          PROJECT_PATH),
      "-cp",
      String.format("%s/paths.tsv", PROJECT_PATH),
      "-i",
      "com.uber.nullaway.annotations.Initializer",
      "-n",
      "javax.annotation.Nullable",
      "-cn",
      "NULLAWAY",
      "-app",
      benchmark.annotatedPackage,
      "-di", // deactivate inference
      "-rrem", // resolve remaining errors
      isBaseline ? "basic" : "advanced",
      //             "-rboserr", // redirect build output stream and error stream
      "--depth",
      "6"
    };
    Config config = new Config(argsArray);
    config.benchmarkName = benchmarkName;
    config.benchmarkPath = PROJECT_PATH;

    System.out.println("Running on branch name: " + config.branchName());
    System.out.println("Starting annotator...");
    // reset git repo
    try (GitUtility git = GitUtility.instance(config)) {
      git.resetHard();
      git.pull();
      git.checkoutBranch("nimak/auto-code-fix");
      git.resetHard();
      git.pull();
      git.deleteLocalBranch(config.branchName());
      git.deleteRemoteBranch(config.branchName());
      git.createAndCheckoutBranch(config.branchName());
      git.pushBranch(config.branchName());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    // Start annotator
    Annotator annotator = new Annotator(config);
    annotator.start();
    if (config.actualRunEnabled()) {
      // copy the logs /tmp/logs directory under Desktop/logs/{project}/{branch}
      Path sourceDir = Paths.get("/tmp/logs");
      Path destDir =
          Paths.get(
              System.getProperty("user.home"),
              "Desktop",
              "logs",
              config.benchmarkName,
              config.branchName().split("/")[1]);
      try (Stream<Path> p = Files.walk(sourceDir)) {
        p.forEach(
            sourcePath -> {
              try {
                Path targetPath = destDir.resolve(sourceDir.relativize(sourcePath));
                if (Files.isDirectory(sourcePath)) {
                  Files.createDirectories(targetPath);
                } else {
                  Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
              } catch (IOException e) {
                System.err.println("Failed to copy: " + sourcePath + " -> " + e.getMessage());
              }
            });
      } catch (IOException e) {
        System.err.println("Failed to copy: " + e.getMessage());
      }
    }
  }

  public static boolean isTheFix(Fix fix) {
    // METHOD_NO_INIT	initializer method does not guarantee @NonNull fields workflowModel (line
    // 190), taskDefinition (line 191), workflowTask (line 192), taskInput (line 193), retryTaskId
    // (line 195), taskId (line 196), deciderService (line 197) are initialized along all
    // control-flow paths (remember to check for exceptions or early returns).
    //	com.netflix.conductor.core.execution.mapper.TaskMapperContext$Builder	Builder()	6696
    //	/home/nima/Developer/nullness-benchmarks/conductor/core/src/main/java/com/netflix/conductor/core/execution/mapper/TaskMapperContext.java	null	null	null	null	null	null
    if (!fix.isOnField()) {
      return false;
    }
    OnField onField = fix.toField();
    return onField.clazz.equals(
            "com.netflix.conductor.core.execution.mapper.TaskMapperContext$Builder")
        && onField.variables.contains("workflowTask");
  }
}
