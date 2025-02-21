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

import edu.ucr.cs.riple.core.util.GitUtility;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/** Starting point. */
public class Main {

  //  /**
  //   * Starting point.
  //   *
  //   * @param args if flag '--path' is found, all configurations will be set up based on the given
  //   *     json file, otherwise they will be set up according to the set of received cli
  // arguments.
  //   */
  //  public static void main(String[] args) {
  //    Config config;
  //    if (args.length == 2 && args[0].equals("--path")) {
  //      config = new Config(Paths.get(args[1]));
  //    } else {
  //      config = new Config(args);
  //    }
  //    Annotator annotator = new Annotator(config);
  //    annotator.start();
  //  }

  public static final String PROJECT_PATH = "/home/nima/Developer/nullness-benchmarks/conductor";
  public static final String BRANCH_NAME = "nimak/auto-code-fix-2";

  public static void main(String[] a) {
    // DELETE LOG:
    try {
      Files.deleteIfExists(Paths.get("/tmp/logs/app.log"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

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
      "/home/nima/Developer/nullness-benchmarks/conductor/annotator-out",
      "-bc",
      "export JAVA_HOME=/usr/lib/jvm/java-1.17.0-openjdk-amd64 && cd /home/nima/Developer/nullness-benchmarks/conductor && ./gradlew clean conductor-core:compileJava --rerun-tasks --no-build-cache",
      "-cp",
      "/home/nima/Developer/nullness-benchmarks/conductor/annotator-out/paths.tsv",
      "-i",
      "com.uber.nullaway.annotations.Initializer",
      "-n",
      "javax.annotation.Nullable",
      "-cn",
      "NULLAWAY",
      "-di", // deactivate inference
      "-rre", // resolve remaining errors
      // "-rboserr", // redirect build output stream and error stream
      "--depth",
      "6"
    };
    Config config = new Config(argsArray);
    // reset git repo
    try (GitUtility git = GitUtility.instance()) {
      git.resetHard();
      git.pull();
      git.checkoutBranch("nimak/auto-code-fix");
      git.resetHard();
      git.pull();
      git.deleteLocalBranch(BRANCH_NAME);
      git.deleteRemoteBranch(BRANCH_NAME);
      git.createAndCheckoutBranch(BRANCH_NAME);
      git.pushBranch(BRANCH_NAME);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    // Start annotator
    Annotator annotator = new Annotator(config);
    annotator.start();
  }
}
