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
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public abstract class BaseCoreTest {

  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  protected final String projectTemplate;
  protected final List<String> modules;
  protected Path projectPath;
  protected Path outDirPath;
  protected CoreTestHelper coreTestHelper;

  public BaseCoreTest(String projectTemplate, List<String> modules) {
    this.projectTemplate = projectTemplate;
    this.modules = modules;
  }

  @Before
  public void setup() {
    outDirPath = Paths.get(temporaryFolder.getRoot().getAbsolutePath());
    projectPath = outDirPath.resolve(projectTemplate);
    Path templates = Paths.get("templates");
    Path pathToUnitTestDir =
        Utility.getPathOfResource(templates.resolve(projectTemplate).toString());
    try {
      FileUtils.deleteDirectory(projectPath.toFile());
      FileUtils.copyDirectory(pathToUnitTestDir.toFile(), projectPath.toFile());
      ProcessBuilder processBuilder = Utility.createProcessInstance();
      processBuilder.directory(projectPath.toFile());
      List<String> commands = new ArrayList<>();
      commands.add("gradle");
      commands.add("wrapper");
      commands.add("--gradle-version");
      commands.add("6.1");
      commands.addAll(Utility.computeConfigPathsWithGradleArguments(outDirPath, modules));
      commands.add(
          "-Plibrary-model-loader-path="
              + Utility.getPathToLibraryModel().resolve("build").resolve("libs"));
      processBuilder.command(commands);
      int success = processBuilder.start().waitFor();
      if (success != 0) {
        throw new RuntimeException("Unable to create Gradle Wrapper.");
      }
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException("Preparation for test failed", e);
    }
    coreTestHelper = new CoreTestHelper(projectPath, outDirPath, modules);
  }
}
