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

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

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
  //    System.out.println("NEW VERSION");
  //    Config config;
  //    if (args.length == 2 && args[0].equals("--path")) {
  //      config = new Config(Paths.get(args[1]));
  //    } else {
  //      config = new Config(args);
  //    }
  //    Annotator annotator = new Annotator(config);
  //    annotator.start();
  //  }

  public static void main(String[] args) {
    final String dir = "/tmp/ucr-tainting/type-arg-test";

    try (FileWriter writer = new FileWriter(dir + "/paths.tsv")) {
      writer.write(
          "/tmp/ucr-tainting/type-arg-test/taint.xml\t/tmp/ucr-tainting/type-arg-test/scanner.xml");
    } catch (Exception e) {
      e.printStackTrace();
    }

    List<String> newArgs = new ArrayList<>();
    newArgs.addAll(List.of("-d", dir));
    newArgs.addAll(
        List.of("-bc", "cd /home/nima/Desktop/TestTypeArg && ./gradlew compileJava --rerun-tasks"));
    newArgs.addAll(List.of("-cp", "/tmp/ucr-tainting/type-arg-test/paths.tsv"));
    newArgs.addAll(List.of("-i", "edu.ucr.Initializer"));
    newArgs.addAll(List.of("-n", "edu.ucr.cs.riple.taint.ucrtainting.qual.RUntainted"));
    newArgs.addAll(List.of("-cn", "UCRTaint"));
    newArgs.addAll(List.of("--depth", "1"));
    // Uncomment to disable outer loop
    newArgs.add("-dol");
    // Uncomment to see build output
    // newArgs.add("-rboserr");
    // Uncomment to disable parallel processing
    // newArgs.add("--disable-parallel-processing");
    Config config;
    config = new Config(newArgs.toArray(String[]::new));
    Annotator annotator = new Annotator(config);
    annotator.start();
  }
}
