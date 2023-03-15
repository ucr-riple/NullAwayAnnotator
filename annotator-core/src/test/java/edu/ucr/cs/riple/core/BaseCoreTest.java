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

package edu.ucr.cs.riple.core;

import edu.ucr.cs.riple.core.tools.CoreTestHelper;
import edu.ucr.cs.riple.core.tools.Utility;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Base class for all core tests. */
@RunWith(JUnit4.class)
public abstract class BaseCoreTest {

  /** Temporary folder for each test. */
  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();
  /** Name of the used project template */
  protected final String projectTemplate;
  /** List of modules to be tested */
  protected final List<String> modules;
  /** Path to the unit test project */
  protected Path unitTestProjectPath;
  /** Path to the output directory */
  protected Path outDirPath;
  /** Helper class for core tests */
  protected CoreTestHelper coreTestHelper;

  public BaseCoreTest(String projectTemplate, List<String> modules) {
    this.projectTemplate = projectTemplate;
    this.modules = modules;
  }

  @Before
  public void setup() {
    outDirPath = Paths.get(temporaryFolder.getRoot().getAbsolutePath());
    unitTestProjectPath = outDirPath.resolve(projectTemplate);
    Path templates = Paths.get("templates");
    Path pathToUnitTestDir =
        Utility.getPathOfResource(templates.resolve(projectTemplate).toString());
    Path repositoryDirectory = Paths.get(System.getProperty("user.dir")).getParent();
    try {
      // Create a separate library models loader to avoid races between unit tests.
      FileUtils.copyDirectory(
          repositoryDirectory.toFile(), outDirPath.resolve("Annotator").toFile());
      FileUtils.deleteDirectory(unitTestProjectPath.toFile());
      FileUtils.copyDirectory(pathToUnitTestDir.toFile(), unitTestProjectPath.toFile());
      // Copy using gradle wrappers.
      FileUtils.copyFile(
          repositoryDirectory.resolve("gradlew").toFile(),
          unitTestProjectPath.resolve("gradlew").toFile());
      FileUtils.copyFile(
          repositoryDirectory.resolve("gradlew.bat").toFile(),
          unitTestProjectPath.resolve("gradlew.bat").toFile());
      FileUtils.copyDirectory(
          repositoryDirectory.resolve("gradle").toFile(),
          unitTestProjectPath.resolve("gradle").toFile());
    } catch (IOException e) {
      throw new RuntimeException("Preparation for test failed", e);
    }
    coreTestHelper = new CoreTestHelper(unitTestProjectPath, outDirPath, modules);
  }
}
