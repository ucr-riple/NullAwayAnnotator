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

package edu.ucr.cs.riple.injector.tools;

import static edu.ucr.cs.riple.injector.tools.Utility.pathOf;
import static org.junit.Assert.fail;

import com.github.javaparser.ParserConfiguration;
import edu.ucr.cs.riple.injector.Injector;
import edu.ucr.cs.riple.injector.changes.ASTChange;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;

public class InjectorTestHelper {

  private final List<ASTChange> changes;
  private final List<String> files;
  private final Path rootPath;

  public InjectorTestHelper(Path path) {
    this.rootPath = path;
    this.changes = new ArrayList<>();
    this.files = new ArrayList<>();
    ParserConfiguration parserConfiguration = new ParserConfiguration();
    parserConfiguration.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
    makeDirectories();
  }

  public InjectorTestHelperOutput addInput(String path, String... input) {
    writeToFile(pathOf(rootPath.resolve("src"), path), input);
    files.add(path);
    return new InjectorTestHelperOutput(this, rootPath, path);
  }

  public InjectorTestHelperOutput addInputSourceFile(String path, String inputFilePath) {
    writeToFile(pathOf(rootPath.resolve("src"), path), readLinesOfFileFromResource(inputFilePath));
    files.add(path);
    return new InjectorTestHelperOutput(this, rootPath, path);
  }

  public InjectorTestHelper addChanges(ASTChange... changes) {
    Arrays.stream(changes)
        .sequential()
        .forEach(
            change ->
                change.getLocation().path =
                    rootPath.resolve("src").resolve(change.getLocation().path));
    this.changes.addAll(Arrays.asList(changes));
    return this;
  }

  public void start() {
    Injector injector = new Injector(ParserConfiguration.LanguageLevel.JAVA_17);
    Method method;
    try {
      // method is private, so we need to make it accessible
      method = Injector.class.getDeclaredMethod("start", Set.class);
      method.setAccessible(true);
      method.invoke(injector, Set.copyOf(changes));
    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    for (String key : files) {
      try {
        String found =
            FileUtils.readFileToString(
                pathOf(rootPath.resolve("src"), key).toFile(), Charset.defaultCharset());
        String expected =
            FileUtils.readFileToString(
                pathOf(rootPath.resolve("expected"), key).toFile(), Charset.defaultCharset());
        if (!expected.equals(found)) {
          fail("Expected:\n" + expected + "\nBut found:\n" + found);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private String[] readLinesOfFileFromResource(String path) {
    try {
      return FileUtils.readLines(
              Paths.get(
                      Objects.requireNonNull(getClass().getClassLoader().getResource(path))
                          .getFile())
                  .toFile(),
              Charset.defaultCharset())
          .toArray(new String[0]);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void makeDirectories() {
    Stream.of("src", "expected")
        .forEach(
            name -> {
              Path pathToDirectory = rootPath.resolve(name);
              try {
                Files.createDirectories(pathToDirectory);
              } catch (IOException e) {
                throw new RuntimeException("Could not create the directories for name: " + name);
              }
            });
  }

  void writeToFile(Path path, String[] input) {
    try {
      Files.createDirectories(path.getParent());
      FileUtils.writeLines(path.toFile(), Arrays.asList(input));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public class InjectorTestHelperOutput {

    private final InjectorTestHelper injectorTestHelper;
    private final String inputFile;
    private final Path root;

    InjectorTestHelperOutput(
        InjectorTestHelper injectorTestHelper, Path root, String inputFilePath) {
      this.root = root;
      this.inputFile = inputFilePath;
      this.injectorTestHelper = injectorTestHelper;
    }

    public InjectorTestHelper expectOutput(String... input) {
      writeToFile(pathOf(root.resolve("expected"), inputFile), input);
      return injectorTestHelper;
    }

    public InjectorTestHelper expectOutputFile(String pathToOutputFile) {
      writeToFile(
          pathOf(root.resolve("expected"), inputFile),
          readLinesOfFileFromResource(pathToOutputFile));
      return injectorTestHelper;
    }
  }
}
