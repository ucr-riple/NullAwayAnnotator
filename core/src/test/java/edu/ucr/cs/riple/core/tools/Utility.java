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

package edu.ucr.cs.riple.core.tools;

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
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class Utility {

  public static void copyDirectory(Path srcPath, Path destPath) {
    String srcDir = srcPath.toString();
    String destDir = destPath.toString();
    try (Stream<Path> paths = Files.walk(srcPath)) {
      paths.forEach(
          source -> {
            Path destination = Paths.get(destDir, source.toString().substring(srcDir.length()));
            try {
              Files.copy(source, destination);
            } catch (IOException e) {
              e.printStackTrace();
            }
          });
    } catch (IOException e) {
      throw new RuntimeException("Failed to copy " + srcDir + " into: " + destDir, e);
    }
  }

  public static Path getPathOfResource(String relativePath) {
    return Paths.get(
        Objects.requireNonNull(Utility.class.getClassLoader().getResource(relativePath)).getFile());
  }

  public static String[] readLinesOfFileFromResource(String path) {
    BufferedReader reader;
    List<String> lines = new ArrayList<>();
    try {
      reader = new BufferedReader(new FileReader(getPathOfResource(path).toFile()));
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

  public static String changeDirCommand(String path) {
    String os = System.getProperty("os.name").toLowerCase();
    return (os.startsWith("windows") ? "dir" : "cd") + " " + path;
  }

  public static void writeToFile(Path path, String content) {
    try {
      Files.createDirectories(path.getParent());
      try (Writer writer = Files.newBufferedWriter(path, Charset.defaultCharset())) {
        writer.write(content);
        writer.flush();
      }
    } catch (IOException e) {
      throw new RuntimeException("Something terrible happened.", e);
    }
  }

  public static ProcessBuilder createProcessInstance() {
    ProcessBuilder pb = new ProcessBuilder();
    String os = System.getProperty("os.name").toLowerCase();
    return os.startsWith("windows") ? pb.command("cmd.exe", "/c") : pb.command("bash", "-c");
  }
}
