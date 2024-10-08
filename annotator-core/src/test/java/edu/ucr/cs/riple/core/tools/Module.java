package edu.ucr.cs.riple.core.tools;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;
import org.apache.commons.io.FileUtils;

/**
 * Container class for a module in the project. This class is responsible for collecting classes
 * contents defined in the unit test and writing it to the source directory of the module.
 */
public class Module {

  /** Name of the module. */
  final String name;

  /**
   * Path to the source directory of the module. All classes must be under test package directory.
   */
  final Path srcSet;

  /**
   * Reference to {@link ProjectBuilder} to preserve a builder pattern and return to the control
   * back to it.
   */
  final ProjectBuilder projectBuilder;

  public Module(ProjectBuilder projectBuilder, String name, Path pathToProject) {
    this.projectBuilder = projectBuilder;
    this.name = name;
    this.srcSet = getPathToSrcSet(pathToProject, name);
    if (!this.srcSet.toFile().mkdirs()) {
      throw new RuntimeException("Failed to create directory: " + this.srcSet);
    }
  }

  /**
   * Adds source code received as lines at the given output path.
   *
   * @param path Relative path to the file to be created from root source directory.
   * @param lines Lines of source code to be written.
   * @return This instance of {@link Module}.
   */
  public Module withSourceLines(String path, String... lines) {
    try {
      FileUtils.writeLines(srcSet.resolve(path).toFile(), Arrays.asList(lines));
    } catch (IOException e) {
      throw new RuntimeException("Failed to write line at: " + path, e);
    }
    return this;
  }

  /**
   * Adds source code which is read from a file resource at the given output path.
   *
   * @param path Relative path to the file to be created from root source directory.
   * @param inputFilePath Path to the file in resources.
   * @return This instance of {@link Module}.
   */
  public Module withSourceFile(String path, String inputFilePath) {
    try {
      return withSourceLines(
          path,
          FileUtils.readFileToString(
              Utility.getPathOfResource(inputFilePath).toFile(), Charset.defaultCharset()));
    } catch (IOException e) {
      throw new RuntimeException("Failed to add source input", e);
    }
  }

  /**
   * Adds source code which is read from a directory resources at the given output path.
   *
   * @param path Relative path to the directory to be created from root source directory.
   * @param inputDirectoryPath Path to the directory in resources.
   * @return This instance of {@link Module}.
   */
  public Module withSourceDirectory(String path, String inputDirectoryPath) {
    Path dir = Utility.getPathOfResource(inputDirectoryPath);
    try {
      FileUtils.copyDirectory(dir.toFile(), srcSet.getParent().resolve(path).toFile());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Adds a dependency to the module.
   *
   * @param name Name of the module to be added as a dependency.
   * @return This instance of {@link Module}.
   */
  public Module withDependency(String name) {
    return projectBuilder.addModule(name);
  }

  /** Finalizes the module construction and returns the control to {@link ProjectBuilder}. */
  public CoreTestHelper withExpectedReports(TReport... reports) {
    return projectBuilder.exitProjectConstruction().addExpectedReports(reports);
  }

  /**
   * Finalizes the module construction and returns the control to {@link ProjectBuilder} with no
   * reports.
   */
  public CoreTestHelper expectNoReport() {
    return projectBuilder.exitProjectConstruction().addExpectedReports();
  }

  /**
   * Gets the path to the source set of the given module. All classes must be under package name
   * test.
   *
   * @param pathToProject Path to the root of the project.
   * @param moduleName Name of the module.
   * @return Path to the source set of the given module under test package.
   */
  private static Path getPathToSrcSet(Path pathToProject, String moduleName) {
    return pathToProject
        .resolve(moduleName)
        .resolve("src")
        .resolve("main")
        .resolve("java")
        .resolve("test");
  }

  @Override
  public String toString() {
    return name;
  }
}
