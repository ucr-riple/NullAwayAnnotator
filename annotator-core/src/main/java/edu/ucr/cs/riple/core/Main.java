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

import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.injector.location.OnMethod;
import java.util.Set;

/** Starting point. */
public class Main {

  //  /**
  //   * Starting point.
  //   *
  //   * @param args if flag "--path" is found, all configurations will be set up based on the given
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

  public static void main(String[] args) {
    args =
        new String[] {
          //            "-d",
          //            "/home/nima/Developer/taint-benchmarks/struts/annotator-out/core",
          //            "-bc",
          //            "java_17 && cd /home/nima/Developer/taint-benchmarks/struts &&
          // /home/nima/Documents/environments/maven/apache-maven-3.9.1/bin/mvn clean compile -pl
          // core",
          //            "-cp",
          //            "/home/nima/Developer/taint-benchmarks/struts/annotator-out/core/paths.tsv",
          //            "-n",
          //            "edu.ucr.cs.riple.taint.ucrtainting.qual.RUntainted",
          //            "-i",
          //            "edu.ucr.cs.riple.taint.ucrtainting.qual.Init",
          //            "-cn",
          //            "UCRTaint",
          //            "-rboserr"
          "-d",
          "/Users/nima/Developer/logisim-evolution/annotator-out/",
          "-bc",
          "export JAVA_HOME=`/usr/libexec/java_home -v 17` && cd /Users/nima/Developer/logisim-evolution && ./gradlew compileJava --rerun-tasks",
          "-cp",
          "/Users/nima/Developer/logisim-evolution/annotator-out//paths.tsv",
          "-i",
          "edu.ucr.Initializer",
          "-n",
          "edu.ucr.cs.riple.taint.ucrtainting.qual.RUntainted",
          "-cn",
          "UCRTaint",
          "--depth",
          "2",
          "-dol",
          "-rboserr",
          "--disable-parallel-processing"
        };
    Config config = new Config(args);
    Annotator annotator = new Annotator(config);
    annotator.start();
  }

  public static boolean isTheFix(Set<Fix> fixes) {
    if (fixes.size() != 1) {
      return false;
    }
    Fix fix = fixes.iterator().next();
    //
    // "annotation":"untainted","location":{"path":"/home/nima/Developer/taint-benchmarks/logisim-evolution/src/main/java/com/cburch/logisim/util/LineBuffer.java","type-variable-position":[[1,0]],"method":"get()","kind":"METHOD","class":"com.cburch.logisim.util.LineBuffer"}}]},
    if (!fix.isOnMethod()) {
      return false;
    }
    OnMethod onMethod = fix.toMethod();
    if (!onMethod.method.equals("get()")) {
      return false;
    }
    if (!onMethod.clazz.equals("com.cburch.logisim.util.LineBuffer")) {
      return false;
    }
    return true;
  }
}
