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
import edu.ucr.cs.riple.core.tools.Utility;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class BasicTest {

  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  private Path projectPath = Paths.get("/tmp/NullAwayFix").resolve("unittest");
  private Path outDirPath = Paths.get("/tmp/NullAwayFix").resolve("unittest");

  @Before
  public void setup() {
    Path pathToUnitTestDir = Utility.getPathOfResource("unittest");
    Utility.copyDirectory(pathToUnitTestDir, projectPath);
    ProcessBuilder processBuilder = Utility.createProcessInstance();
    processBuilder.directory(projectPath.toFile());
    processBuilder.command("gradle", "wrapper", "--gradle-version", "6.1");
    try {
      processBuilder.start().waitFor();
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void field_test() {
    CoreTestHelper coreTestHelper = new CoreTestHelper(projectPath, outDirPath);
  }
}
