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

package edu.ucr.cs.riple.scanner.tools;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.CompilationTestHelper;
import edu.ucr.cs.riple.scanner.TypeAnnotatorScanner;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SerializationTestHelper<T extends Display> {

  private final Path outputDir;
  private ImmutableList<T> expectedOutputs;
  private CompilationTestHelper compilationTestHelper;
  private DisplayFactory<T> factory;
  private String fileName;
  private String header;

  private Path outputFilePath;

  public SerializationTestHelper(Path outputDir) {
    this.outputDir = outputDir;
  }

  /**
   * Adds source code to the list of inputs. This method is part of the builder pattern.
   *
   * @param path Relative path to src directory where the given source code should exist.
   * @param lines Lines of source code.
   * @return Receiver of the call.
   */
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public SerializationTestHelper<T> addSourceLines(String path, String... lines) {
    compilationTestHelper.addSourceLines(path, lines);
    return this;
  }

  /**
   * Adds source code to the list of inputs. This method is part of the builder pattern.
   *
   * @param path Path to resource.
   * @return Receiver of the call.
   */
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public SerializationTestHelper<T> addSourceFile(String path) {
    // This class is inside "tools" package, which means compilationTestHelper will try to use a
    // corresponding "tools" directory within test/resources/[...]/scanner. We prepend ".." to the
    // path, to escape this non-existent directory.
    path = "../" + path;
    compilationTestHelper.addSourceFile(path);
    return this;
  }

  /**
   * Sets the expected output. Any unseen / unexpected output will result to a failure in the test.
   * This method is part of the builder pattern.
   *
   * @param outputs Expected output.
   * @return Receiver of the call.
   */
  @SafeVarargs
  public final SerializationTestHelper<T> setExpectedOutputs(T... outputs) {
    this.expectedOutputs = ImmutableList.copyOf(outputs);
    return this;
  }

  /**
   * If called, no output should be expected while running the test. This method is part of the
   * builder pattern.
   *
   * @return Receiver of the call.
   */
  public SerializationTestHelper<T> expectNoOutput() {
    this.expectedOutputs = ImmutableList.of();
    return this;
  }

  /**
   * Creates the actual {@link TypeAnnotatorScanner} with the given arguments. This method is part
   * of the builder pattern and should be called before any other method.
   *
   * @return Receiver of the call.
   */
  public SerializationTestHelper<T> setArgs(List<String> args) {
    compilationTestHelper =
        CompilationTestHelper.newInstance(TypeAnnotatorScanner.class, getClass()).setArgs(args);
    return this;
  }

  /**
   * Sets factory. This method is part of the builder pattern.
   *
   * @param factory Factory instance.
   * @return Receiver of the call.
   */
  public SerializationTestHelper<T> setFactory(DisplayFactory<T> factory) {
    this.factory = factory;
    return this;
  }

  /**
   * Sets file name and the expected header of output file. This method is part of the builder
   * pattern.
   *
   * @param fileName Output file name.
   * @param header Expected header.
   * @return Receiver of the call.
   */
  public SerializationTestHelper<T> setOutputFileNameAndHeader(String fileName, String header) {
    this.fileName = fileName;
    this.header = header;
    return this;
  }

  private void prepareTest() {
    Preconditions.checkNotNull(factory, "Factory cannot be null");
    Preconditions.checkNotNull(fileName, "File name cannot be null");
    outputFilePath = outputDir.resolve(fileName);
    try {
      Files.deleteIfExists(outputFilePath);
    } catch (IOException ignored) {
      throw new RuntimeException("Failed to delete older file at: " + outputFilePath);
    }
  }

  /**
   * Runs the testing with expecting to encounter a specific error.
   *
   * @param exception Expected Exception to be raised by running the test.
   * @param expectedErrorMessage Expected message to be printed by running the test.
   */
  public void doTestWithExpectingError(
      Class<? extends Exception> exception, String expectedErrorMessage) {
    String fullExpectedMessage = "Caused by: " + exception.getName() + ": " + expectedErrorMessage;
    prepareTest();
    AssertionError ex = assertThrows(AssertionError.class, () -> compilationTestHelper.doTest());
    assert ex.getMessage().contains(fullExpectedMessage);
  }

  /** Runs the test. */
  public void doTest() {
    prepareTest();
    compilationTestHelper.doTest();
    List<T> actualOutputs = readActualOutputs();
    compare(actualOutputs);
  }

  private void compare(List<T> actualOutput) {
    List<T> notFound = new ArrayList<>();
    for (T o : expectedOutputs) {
      if (!actualOutput.contains(o)) {
        notFound.add(o);
      } else {
        actualOutput.remove(o);
      }
    }
    if (notFound.size() == 0 && actualOutput.size() == 0) {
      return;
    }
    StringBuilder errorMessage = new StringBuilder();
    if (notFound.size() != 0) {
      errorMessage
          .append(notFound.size())
          .append(" expected outputs were NOT found:")
          .append("\n")
          .append(notFound.stream().map(T::toString).collect(Collectors.toList()))
          .append("\n");
    }
    if (actualOutput.size() != 0) {
      errorMessage
          .append(actualOutput.size())
          .append(" unexpected outputs were found:")
          .append("\n")
          .append(actualOutput.stream().map(T::toString).collect(Collectors.toList()))
          .append("\n");
    }
    fail(errorMessage.toString());
  }

  private List<T> readActualOutputs() {
    List<T> outputs = new ArrayList<>();
    BufferedReader reader;
    try {
      reader = Files.newBufferedReader(outputFilePath, Charset.defaultCharset());
      String actualHeader = reader.readLine();
      if (!header.equals(actualHeader)) {
        fail(
            "Expected header of "
                + outputFilePath.getFileName()
                + " to be: "
                + header
                + "\nBut found: "
                + actualHeader);
      }
      String line = reader.readLine();
      while (line != null) {
        T output = factory.fromValuesInString(line.split("\\t"));
        outputs.add(output);
        line = reader.readLine();
      }
      reader.close();
    } catch (IOException e) {
      throw new RuntimeException("Error happened in reading the outputs.", e);
    }
    return outputs;
  }
}
