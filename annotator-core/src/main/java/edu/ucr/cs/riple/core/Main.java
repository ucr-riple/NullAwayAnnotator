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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import edu.ucr.cs.riple.core.util.GitUtility;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Objects;

/** Starting point. */
public class Main {

  public static final int VERSION = 2;

  public static class Benchmark {
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
    benchmarks =
        Map.of(
            "libgdx", new Benchmark("com.badlogic.gdx", "libgdx", "compileJava"),
            "zuul", new Benchmark("com.netflix", "zuul", "zuul-core:compileJava"));
  }

  // PROJECT SPECIFIC CONFIGURATION
  // Ubuntu
  public static final boolean DEBUG_MODE = false;
  public static final String DEBUG_LINE = "return (Class<T>) PRIMITIVE_TYPES.get(clazz);";

  public static void main(String[] args) {
    String benchmarkName = args[0];
    boolean isBaseline = args.length > 1 && args[1].equals("baseline");
    String branchName = String.format("nimak/agentic-%s-%s", isBaseline ? "basic" : "advanced", 2);
    Path logRootPath = configureLogging(benchmarkName, branchName);

    System.clearProperty("ANNOTATOR_TEST_MODE");
    System.out.println(
        "Running "
            + benchmarkName
            + " benchmark in "
            + (isBaseline ? "baseline" : "advanced")
            + " mode.");
    Benchmark benchmark = benchmarks.get(benchmarkName);
    if (benchmark == null) {
      throw new IllegalArgumentException("Unknown benchmark: " + benchmarkName);
    }
    String PROJECT_PATH = "/home/nima/Developer/nullness-benchmarks/" + benchmark.path;
    deleteOutDir(benchmark);
    String[] argsArray = {
      "-d",
      String.format("%s/annotator-out", PROJECT_PATH),
      "-bc",
      String.format(
          "export JAVA_HOME=/usr/lib/jvm/java-1.17.0-openjdk-amd64 && cd %s && ANDROID_HOME=/home/nima/Android/Sdk ./gradlew clean %s --rerun-tasks --no-build-cache",
          PROJECT_PATH, benchmark.buildCommand),
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
    config.logPath = logRootPath.resolve("app.log");
    config.commitHashPath = logRootPath.resolve("commits.tsv");
    config.timerPath = logRootPath.resolve("timers.tsv");

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
  }

  public static void deleteOutDir(Benchmark benchmark) {
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
  }

  public static Path configureLogging(String benchmarkName, String branchName) {
    System.out.println(
        "Configuring logging for benchmark: " + benchmarkName + ", branch: " + branchName);
    Path root =
        Paths.get(
            System.getProperty("user.home"),
            "Desktop",
            "logs",
            benchmarkName,
            branchName.split("/")[1]);
    System.out.println("Root path for logs: " + root);
    // Delete log
    try {
      if (Files.exists(root)) {
        MoreFiles.deleteRecursively(root, RecursiveDeleteOption.ALLOW_INSECURE);
      }
      Files.createDirectories(root);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    String filePath = root.resolve("app.log").toString();

    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    context.reset();

    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
    encoder.setContext(context);
    encoder.setPattern("%d{HH:mm:ss.SSS} %-5level %class.%method%n%msg%n");
    encoder.start();

    FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
    fileAppender.setContext(context);
    fileAppender.setFile(filePath);
    fileAppender.setEncoder(encoder);
    fileAppender.start();

    Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
    rootLogger.detachAndStopAllAppenders();
    rootLogger.addAppender(fileAppender);
    rootLogger.setLevel(Level.WARN);  // Suppress most external INFO logs

    Logger myLogger = context.getLogger("edu.ucr.cs.riple"); // or your base package
    myLogger.setLevel(Level.TRACE);  // See your trace/debug logs
    // or WARN if too noisy


    File file = new File(filePath);
    System.out.println("Log path exists: " + file.exists());
    System.out.println("Can write to log path: " + file.canWrite());

    System.out.println("Testing...");
    LoggerFactory.getLogger(Main.class).trace("TEST TRACE LOG");
    System.out.println("Test log written to: " + filePath);

    return root;
  }
}
