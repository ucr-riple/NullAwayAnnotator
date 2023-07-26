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

package edu.ucr.cs.riple.core.tools;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper class to create a project with multiple modules. Each module has a source directory which
 * contains all the source files of the module.
 */
public class ProjectBuilder {

  /** Set of modules in the project. */
  private final List<Module> modules;
  /** Path to the project. */
  private final Path pathToProject;
  /**
   * Reference to {@link CoreTestHelper} to preserve a builder pattern and return to the control
   * back to it.
   */
  private final CoreTestHelper coreTestHelper;

  public ProjectBuilder(CoreTestHelper coreTestHelper, Path pathToProject) {
    this.coreTestHelper = coreTestHelper;
    this.pathToProject = pathToProject;
    this.modules = new ArrayList<>();
  }

  public Module onTarget() {
    return addModule("Target");
  }

  /**
   * Creates an empty module in the project.
   *
   * @return Reference to the created module.
   */
  public Module onEmptyProject() {
    Module module = new Module(this, "Empty", pathToProject);
    modules.add(module);
    return module;
  }

  /**
   * Creates a module in the project.
   *
   * @param name Name of the module.
   * @return Reference to the created module.
   */
  Module addModule(String name) {
    Module module = new Module(this, name, pathToProject);
    modules.add(module);
    // add module to settings.gradle
    Utility.appendToFile(
        pathToProject.resolve("settings.gradle"), String.format("include '%s'\n", module));
    return module;
  }

  /**
   * Getter for modules in the project.
   *
   * @return List of modules in the project.
   */
  public List<Module> getModules() {
    return modules;
  }

  /** Finalizes the project creation and returns the control back to {@link CoreTestHelper}. */
  CoreTestHelper exitProjectConstruction() {
    return coreTestHelper;
  }

  /**
   * Computes the build command for the target module. It includes, changing directory command from
   * root to project root dir, command to compile the project and the computed paths to config files
   * which will be passed through gradle command line arguments.
   *
   * @param outDirPath Path to serialization output directory,
   * @return The command to build the project including the command line arguments, this command can
   *     * be executed from any directory.
   */
  public String computeTargetBuildCommand(Path outDirPath) {
    return String.format(
        "%s && ./gradlew %s %s -Plibrary-model-loader-path=%s --rerun-tasks",
        Utility.changeDirCommand(pathToProject),
        computeCompileGradleCommandForModules(modules.subList(0, 1)),
        String.join(" ", Utility.computeConfigPathsWithGradleArguments(outDirPath, modules)),
        Utility.getPathToLibraryModel(outDirPath)
            .resolve(Paths.get("build", "libs", "librarymodel.jar")));
  }

  /**
   * Computes the build command for the target project. It includes, changing directory command from
   * root to project root dir, command to compile the project, command to update library model
   * loader jar and the computed paths to config files which will be passed through gradle command
   * line arguments.
   *
   * @param outDirPath Path to serialization output directory,
   * @return The command to build the project including the command line arguments, this command can
   *     * be executed from any directory.
   */
  public String computeTargetBuildCommandWithLibraryModelLoaderDependency(Path outDirPath) {
    return String.format(
        "%s && ./gradlew library-model-loader:jar --rerun-tasks && %s && ./gradlew %s %s -Plibrary-model-loader-path=%s --rerun-tasks",
        Utility.changeDirCommand(outDirPath.resolve("Annotator")),
        Utility.changeDirCommand(pathToProject),
        computeCompileGradleCommandForModules(modules.subList(0, 1)),
        String.join(" ", Utility.computeConfigPathsWithGradleArguments(outDirPath, modules)),
        Utility.getPathToLibraryModel(outDirPath)
            .resolve(Paths.get("build", "libs", "librarymodel.jar")));
  }

  /**
   * Computes the build command for downstream dependencies. It includes, changing directory command
   * from root to project root dir, command to compile the project, command to update library model
   * loader jar and the computed paths to config files which will be passed through gradle command
   * line arguments.
   *
   * @param outDirPath Path to serialization output directory,
   * @return The command to build the project including the command line arguments, this command can
   *     * be executed from any directory.
   */
  public String computeDownstreamDependencyBuildCommandWithLibraryModelLoaderDependency(
      Path outDirPath) {
    return String.format(
        "%s && ./gradlew library-model-loader:jar --rerun-tasks && %s && ./gradlew %s %s -Plibrary-model-loader-path=%s --rerun-tasks",
        Utility.changeDirCommand(outDirPath.resolve("Annotator")),
        Utility.changeDirCommand(pathToProject),
        computeCompileGradleCommandForModules(modules.subList(1, modules.size())),
        String.join(" ", Utility.computeConfigPathsWithGradleArguments(outDirPath, modules)),
        Utility.getPathToLibraryModel(outDirPath)
            .resolve(Paths.get("build", "libs", "librarymodel.jar")));
  }

  /**
   * Computes the gradle compile command for the given modules. {e.g. :module1:compileJava
   * :module2:compileJava ...}
   *
   * @param modules List of modules to compile.
   * @return The gradle compile command for the given modules.
   */
  private String computeCompileGradleCommandForModules(List<Module> modules) {
    return modules.stream()
        .map(module -> String.format(":%s:compileJava", module))
        .collect(Collectors.joining(" "));
  }
}
