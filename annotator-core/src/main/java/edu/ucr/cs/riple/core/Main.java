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
import java.util.stream.Stream;

/** Starting point. */
public class Main {

  //    /**
  //     * Starting point.
  //     *
  //     * @param args if flag '--path' is found, all configurations will be set up based on the
  // given
  //     *     json file, otherwise they will be set up according to the set of received cli
  //   arguments.
  //     */
  //    public static void main(String[] args) {
  //      Config config;
  //      if (args.length == 2 && args[0].equals("--path")) {
  //        config = new Config(Paths.get(args[1]));
  //      } else {
  //        config = new Config(args);
  //      }
  //      Annotator annotator = new Annotator(config);
  //      annotator.start();
  //    }

  // Mac
  //  public static final String PROJECT_PATH = "/Users/nima/Desktop/conductor";
  // Ubuntu
  public static final boolean TEST_MODE = System.getProperty("ANNOTATOR_TEST_MODE") != null;
  public static final boolean DEBUG_MODE = false;
  public static final String PROJECT_PATH = "/home/nima/Developer/nullness-benchmarks/litiengine";
  public static final String BENCHMARK_NAME = PROJECT_PATH.split("/")[PROJECT_PATH.split("/").length - 1];
  public static final String BRANCH_NAME = "nimak/auto-code-fix-5";
  public static final Path LOG_PATH = Paths.get("/tmp/logs/app.log");
  public static final String ANNOTATED_PACKAGE = "de.gurkenlabs.litiengine";

  public static void main(String[] a) {
//    System.clearProperty("ANNOTATOR_TEST_MODE");
//    // DELETE LOG:
//    try {
//      Files.deleteIfExists(LOG_PATH);
//    } catch (IOException e) {
//      throw new RuntimeException(e);
//    }
//
//    // delete dir
//    Path outDir = Paths.get(PROJECT_PATH + "/annotator-out/0");
//
//    if (outDir.toFile().exists()) {
//      try {
//        Files.walkFileTree(
//            outDir,
//            new SimpleFileVisitor<>() {
//              @Override
//              public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
//                  throws IOException {
//                Files.delete(file);
//                return FileVisitResult.CONTINUE;
//              }
//
//              @Override
//              public FileVisitResult postVisitDirectory(Path dir, IOException exc)
//                  throws IOException {
//                Files.delete(dir);
//                return FileVisitResult.CONTINUE;
//              }
//            });
//      } catch (IOException e) {
//        throw new RuntimeException(e);
//      }
//    }
//
//    String[] argsArray = {
//      "-d",
//      String.format("%s/annotator-out", PROJECT_PATH),
//      "-bc",
//      // For Ubuntu
//      String.format(
//          "export JAVA_HOME=/usr/lib/jvm/java-1.17.0-openjdk-amd64 && cd %s && ./gradlew clean compileJava --rerun-tasks --no-build-cache",
//          PROJECT_PATH),
//      //      // For Mac
//      //      String.format(
//      //          "JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.0.2.jdk/Contents/Home &&cd %s
//      // && ./gradlew clean conductor-core:compileJava --rerun-tasks --no-build-cache",
//      //          PROJECT_PATH),
//      "-cp",
//      String.format("%s/paths.tsv", PROJECT_PATH),
//      "-i",
//      "com.uber.nullaway.annotations.Initializer",
//      "-n",
//      "javax.annotation.Nullable",
//      "-cn",
//      "NULLAWAY",
//      "-app",
//      ANNOTATED_PACKAGE,
//      "-di", // deactivate inference
//      "-rrem", // resolve remaining errors
//      "advanced",
//      //       "-rboserr", // redirect build output stream and error stream
//      "--depth",
//      "6"
//    };
//    Config config = new Config(argsArray);
//    // reset git repo
//    try (GitUtility git = GitUtility.instance()) {
//      git.resetHard();
//      git.pull();
//      git.checkoutBranch("nimak/auto-code-fix");
//      git.resetHard();
//      git.pull();
//      git.deleteLocalBranch(BRANCH_NAME);
//      git.deleteRemoteBranch(BRANCH_NAME);
//      git.createAndCheckoutBranch(BRANCH_NAME);
//      git.pushBranch(BRANCH_NAME);
//    } catch (Exception e) {
//      throw new RuntimeException(e);
//    }
//    // Start annotator
//    Annotator annotator = new Annotator(config);
//    annotator.start();
    // copy the logs /tmp/logs directory under Desktop/logs/{project}/{branch}
    Path sourceDir = Paths.get("/tmp/logs");
    Path destDir = Paths.get(System.getProperty("user.home"), "Desktop", "logs", BENCHMARK_NAME, BRANCH_NAME.split("/")[1]);
    try(Stream<Path> p = Files.walk(sourceDir)){
      p.forEach(sourcePath -> {
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
    }catch (IOException e){
      System.err.println("Failed to copy: " + e.getMessage());
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
