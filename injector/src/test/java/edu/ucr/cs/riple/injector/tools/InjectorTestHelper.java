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

import static org.junit.Assert.fail;

import edu.ucr.cs.riple.injector.Change;
import edu.ucr.cs.riple.injector.Injector;
import edu.ucr.cs.riple.injector.WorkListBuilder;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@SuppressWarnings("unchecked")
public class InjectorTestHelper {

  private final Map<String, String> fileMap;
  private final ArrayList<Change> changes;
  private Path rootPath;

  public InjectorTestHelper() {
    changes = new ArrayList<>();
    fileMap = new HashMap<>();
  }

  public InjectorTestHelperOutput addInput(String path, String... input) {
    if (rootPath == null) {
      throw new RuntimeException("Root path must be set before calling addInput");
    }
    String pathToInputFile = writeToFile("src/" + path, input);
    return new InjectorTestHelperOutput(this, fileMap, pathToInputFile);
  }

  public InjectorTestHelperOutput addInputSourceFile(String path, String inputFilePath) {
    if (rootPath == null) {
      throw new RuntimeException("Root path must be set before calling addInput");
    }
    String pathToInputFile = writeToFile("src/" + path, readLinesOfFile(inputFilePath));
    return new InjectorTestHelperOutput(this, fileMap, pathToInputFile);
  }

  public InjectorTestHelper addChanges(Change... changes) {
    Arrays.stream(changes)
        .sequential()
        .forEach(
            change ->
                change.location.uri =
                    rootPath
                        .resolve("src")
                        .resolve(change.location.uri)
                        .toAbsolutePath()
                        .toString());
    this.changes.addAll(Arrays.asList(changes));
    return this;
  }

  public InjectorTestHelper setRootPath(String path) {
    this.rootPath = Paths.get(path);
    makeDirectories();
    return this;
  }

  public void start(boolean keepStyle) {
    Injector injector = Injector.builder().setMode(Injector.MODE.TEST).keepStyle(keepStyle).build();
    injector.start(new WorkListBuilder(changes).getWorkLists(), keepStyle);
    for (String key : fileMap.keySet()) {
      String srcFile = readFileToString(key);
      String trimmedSrc = srcFile.replaceAll(" ", "").replaceAll("\n", "").replaceAll("\t", "");
      String destFile = readFileToString(fileMap.get(key));
      String trimmedDest = destFile.replaceAll(" ", "").replaceAll("\n", "").replaceAll("\t", "");
      if (!trimmedSrc.equals(trimmedDest)) {
        System.out.println("FOUND   : " + trimmedSrc);
        System.out.println("EXPECTED: " + trimmedDest);
        fail("\nExpected:\n" + destFile + "\n\nBut found:\n" + srcFile + "\n");
      }
    }
  }

  public void start() {
    start(false);
  }

  private String[] readLinesOfFile(String path) {
    BufferedReader reader;
    List<String> lines = new ArrayList<>();
    try {
      reader =
          new BufferedReader(
              new FileReader(
                  Objects.requireNonNull(getClass().getClassLoader().getResource(path)).getFile()));
      String line = reader.readLine();
      while (line != null) {
        lines.add(line);
        line = reader.readLine();
      }
      reader.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return lines.toArray(new String[0]);
  }

  private void makeDirectories() {
    String[] names = {"src", "out", "expected"};
    for (String name : names) {
      String pathToDirectory = rootPath + "/" + name;
      try {
        Files.createDirectories(Paths.get(pathToDirectory + "/"));
      } catch (IOException e) {
        throw new RuntimeException("Could not create the directories for name: " + name);
      }
    }
  }

  String writeToFile(String relativePath, String[] input) {
    StringBuilder toWrite = new StringBuilder();
    for (String s : input) {
      toWrite.append(s).append("\n");
    }
    return writeToFile(relativePath, toWrite.toString());
  }

  String writeToFile(String relativePath, String input) {
    input = input.replace("\\", "");
    relativePath = rootPath.resolve(relativePath).toAbsolutePath().toString();
    String pathToFileDirectory = relativePath.substring(0, relativePath.lastIndexOf("/"));
    try {
      Files.createDirectories(Paths.get(pathToFileDirectory + "/"));
      try (Writer writer =
          Files.newBufferedWriter(Paths.get(relativePath), Charset.defaultCharset())) {
        writer.write(input);
        writer.flush();
        return relativePath;
      }
    } catch (IOException e) {
      throw new RuntimeException("Something terrible happened.", e);
    }
  }

  private String readFileToString(String path) {
    StringBuilder contentBuilder = new StringBuilder();
    try (Stream<String> stream = Files.lines(Paths.get(path), Charset.defaultCharset())) {
      stream.forEach(s -> contentBuilder.append(s).append("\n"));
      return contentBuilder.toString();
    } catch (FileNotFoundException ex) {
      throw new RuntimeException("Unable to open file: " + path, ex);
    } catch (IOException ex) {
      throw new RuntimeException("Error reading file: " + path, ex);
    }
  }

  public class InjectorTestHelperOutput {

    private final InjectorTestHelper injectorTestHelper;
    private final String inputFile;
    private final Map<String, String> map;

    InjectorTestHelperOutput(
        InjectorTestHelper injectorTestHelper, Map<String, String> map, String inputFile) {
      this.map = map;
      this.inputFile = inputFile;
      this.injectorTestHelper = injectorTestHelper;
    }

    public InjectorTestHelper expectOutput(String path, String... input) {
      String output = writeToFile("expected/" + path, input);
      map.put(inputFile.replace("src", "out"), output);
      return injectorTestHelper;
    }

    public InjectorTestHelper expectOutputFile(String path, String pathToOutputFile) {
      String output = writeToFile("expected/" + path, readLinesOfFile(pathToOutputFile));
      map.put(inputFile.replace("src", "out"), output);
      return injectorTestHelper;
    }
  }
}
