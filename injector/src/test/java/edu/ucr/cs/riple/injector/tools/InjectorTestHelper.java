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

import edu.ucr.cs.riple.injector.Fix;
import edu.ucr.cs.riple.injector.Injector;
import edu.ucr.cs.riple.injector.Report;
import edu.ucr.cs.riple.injector.WorkListBuilder;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

@SuppressWarnings("unchecked")
public class InjectorTestHelper {

  private final Map<String, String> fileMap;
  private final ArrayList<Fix> fixes;
  private String rootPath;

  public InjectorTestHelper() {
    fixes = new ArrayList<>();
    fileMap = new HashMap<>();
  }

  public InjectorTestHelperOutput addInput(String path, String... input) {
    if (rootPath == null || rootPath.equals(""))
      throw new RuntimeException("Root path must be set before calling addInput");
    String inputFile = writeToFile("src/" + path, input);
    return new InjectorTestHelperOutput(this, fileMap, inputFile);
  }

  public InjectorTestHelper addFixes(Fix... fixes) {
    for (Fix f : fixes) f.uri = rootPath.concat("/src/").concat(f.uri);
    this.fixes.addAll(Arrays.asList(fixes));
    return this;
  }

  public InjectorTestHelper setRootPath(String path) {
    this.rootPath = path;
    makeDirectories();
    return this;
  }

  public void start(boolean keepStyle) {
    Injector injector = Injector.builder().setMode(Injector.MODE.TEST).keepStyle(keepStyle).build();
    writeFixes();
    Report report =
        injector.start(new WorkListBuilder(rootPath + "/fix/fixes.json").getWorkLists());
    System.out.println("Report: " + report);
    for (String key : fileMap.keySet()) {
      String srcFile = readFileToString(key);
      String trimmedSrc = srcFile.replace(" ", "").replace("\n", "");
      String destFile = readFileToString(fileMap.get(key));
      String trimmedDest = destFile.replace(" ", "").replace("\n", "");
      if (!trimmedSrc.equals(trimmedDest))
        fail("\nExpected:\n" + destFile + "\n\nBut found:\n" + srcFile + "\n");
    }
  }

  public void start() {
    start(false);
  }

  private void writeFixes() {
    JSONArray array = new JSONArray();
    for (Fix fix : fixes) {
      array.add(fix.getJson());
    }
    JSONObject obj = new JSONObject();
    obj.put("fixes", array);
    writeToFile("fix/fixes.json", obj.toJSONString());
  }

  private void makeDirectories() {
    String[] names = {"src", "out", "expected", "fix"};
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
    for (String s : input) toWrite.append(s).append("\n");
    return writeToFile(relativePath, toWrite.toString());
  }

  String writeToFile(String relativePath, String input) {
    input = input.replace("\\", "");
    relativePath = rootPath.concat("/").concat(relativePath);
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
      throw new RuntimeException("Something terrible happened.");
    }
  }

  private String readFileToString(String path) {
    StringBuilder contentBuilder = new StringBuilder();
    try {
      Stream<String> stream = Files.lines(Paths.get(path), Charset.defaultCharset());
      stream.forEach(s -> contentBuilder.append(s).append("\n"));
      return contentBuilder.toString();
    } catch (FileNotFoundException ex) {
      throw new RuntimeException("Unable to open file: " + path);
    } catch (IOException ex) {
      throw new RuntimeException("Error reading file: " + path);
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
  }
}
