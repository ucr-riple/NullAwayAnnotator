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

import edu.ucr.cs.riple.core.tools.CoreTestHelper;
import edu.ucr.cs.riple.core.tools.TReport;
import edu.ucr.cs.riple.core.tools.Utility;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.injector.location.OnMethod;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class BasicTest {

  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  private Path projectPath;
  private Path outDirPath;

  @Before
  public void setup() {
    outDirPath = Paths.get(temporaryFolder.getRoot().getAbsolutePath());
    projectPath = outDirPath.resolve("unittest");
    Path pathToUnitTestDir = Utility.getPathOfResource("unittest");
    Utility.copyDirectory(pathToUnitTestDir, projectPath);
    ProcessBuilder processBuilder = Utility.createProcessInstance();
    processBuilder.directory(projectPath.toFile());
    processBuilder.command("gradle", "wrapper", "--gradle-version", "6.1");
    File buildFile = projectPath.resolve("build.gradle").toFile();
    try {
      String buildContent = FileUtils.readFileToString(buildFile, Charset.defaultCharset());
      buildContent =
          buildContent.replace(
              "-XepOpt:NullAway:FixSerializationConfigPath=",
              "-XepOpt:NullAway:FixSerializationConfigPath=" + outDirPath.resolve("config.xml"));
      buildContent =
          buildContent.replace(
              "-XepOpt:CSS:ConfigPath=", "-XepOpt:CSS:ConfigPath=" + outDirPath.resolve("css.xml"));
      FileUtils.writeStringToFile(buildFile, buildContent, Charset.defaultCharset());
      processBuilder.start().waitFor();
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException("Preparation for test failed", e);
    }
  }

  @Test
  public void field() {
    CoreTestHelper coreTestHelper = new CoreTestHelper(projectPath, outDirPath);
    coreTestHelper
        .addInputLines("Main.java", "package test;", "public class Main {", "Object field;", "}")
        .addExpectedReports(
            new TReport(new OnField("Main.java", "test.Main", Collections.singleton("field")), -1))
        .start();
  }

  @Test
  public void method() {
    CoreTestHelper coreTestHelper = new CoreTestHelper(projectPath, outDirPath);
    coreTestHelper
        .addInputLines(
            "Main.java",
            "package test;",
            "public class Main {",
            "   Object run() {",
            "     return null;",
            "   }",
            "}")
        .addExpectedReports(new TReport(new OnMethod("Main.java", "test.Main", "run()"), -1))
        .start();
  }
}
