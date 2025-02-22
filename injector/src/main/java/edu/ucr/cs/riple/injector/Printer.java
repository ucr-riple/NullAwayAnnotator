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

package edu.ucr.cs.riple.injector;

import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import edu.ucr.cs.riple.injector.modifications.Modification;
import edu.ucr.cs.riple.injector.offsets.FileOffsetStore;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Applies the text modification instances to source file. Text modifications are applied according
 * to the target element {@link javax.lang.model.element.ElementKind}.
 */
public class Printer {

  /** Path to source file. */
  private final Path path;

  /** Lines of source file. */
  private final List<String> lines;

  /** Offset store for recording changes in source code. */
  private final FileOffsetStore offsetStore;

  public Printer(Path path) {
    this.path = path;
    try {
      lines = Files.readAllLines(path, Charset.defaultCharset());
    } catch (IOException e) {
      throw new RuntimeException("Happened at path: " + path, e);
    }
    this.offsetStore = new FileOffsetStore(lines, path);
  }

  /**
   * Applies the set of modification to source file.
   *
   * @param modifications Set of modifications.
   */
  public void applyModifications(Set<Modification> modifications) {
    modifications =
        modifications.stream()
            .flatMap(modification -> modification.flatten().stream())
            .collect(Collectors.toSet());
    // Modification are sorted to start from the last position, to make the changes ineffective to
    // current computed offsets.
    SortedSet<Modification> reversedSortedSet = new TreeSet<>(Collections.reverseOrder());
    reversedSortedSet.addAll(modifications);
    reversedSortedSet.forEach(modification -> modification.visit(lines, offsetStore));
  }

  /**
   * Adds the set of import declarations to source file.
   *
   * @param tree Compilation unit tree, required to compute the offset of the import declarations.
   * @param imports Set of import declarations to be added.
   */
  public void addImports(CompilationUnit tree, Set<ImportDeclaration> imports) {
    if (imports.isEmpty()) {
      return;
    }
    int line = findStartOffsetForImports(tree);
    imports.forEach(
        importDec -> {
          String toAdd = importDec.toString().strip();
          offsetStore.updateOffsetWithNewLineAddition(line, toAdd.length());
          lines.add(line, toAdd);
        });
  }

  /**
   * Computes the offset where the import declarations should be added. Below is the steps it takes
   * to compute the correct offset. At each step, if the desired location is not found, it goes to
   * following step.
   *
   * <ol>
   *   <li>Immediately after the last existing import statement
   *   <li>Immediately after the package declaration statement
   *   <li>Immediately after the copyright header
   *   <li>Immediately before the first non-empty line in the file
   * </ol>
   *
   * @param tree The compilation unit tree.
   * @return The offset where all the new import declaration statements should be inserted.
   */
  private int findStartOffsetForImports(CompilationUnit tree) {
    // Get position of last import if exists
    Optional<ImportDeclaration> optional = tree.getImports().getLast();
    if (optional.isPresent()) {
      // at least one import statement exist, add imports after that
      Optional<Range> range = optional.get().getRange();
      if (range.isPresent()) {
        return range.get().end.line;
      }
    }
    // No import exists, add below package declaration
    if (tree.getPackageDeclaration().isPresent()) {
      Optional<Range> range = tree.getPackageDeclaration().get().getRange();
      if (range.isPresent()) {
        return range.get().end.line;
      }
    }
    // No package exists, add import under copyright header if exists, otherwise on the first line.
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i).strip();
      if (line.isEmpty()) {
        continue;
      }
      // For copyrights surrounded with "/* **/"
      if (line.startsWith("/*")) {
        while (i < lines.size()) {
          String endLine = lines.get(i);
          if (endLine.contains("*/")) {
            return i + 1;
          }
          i++;
        }
      }
      // For copyrights starting with "//" on each line.
      if (line.startsWith("//")) {
        while (i < lines.size() && lines.get(i).startsWith("//")) {
          i++;
        }
        return i;
      }
      return i;
    }

    throw new RuntimeException(
        "Could not figure out the starting point for imports for file at path: " + path);
  }

  /**
   * Writes the updated lines into the source file.
   *
   * @return offset store corresponding to file changes.
   */
  public FileOffsetStore write() {
    try {
      Files.write(path, lines, Charset.defaultCharset());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return offsetStore;
  }

  /**
   * Deserializes a Path instance from a string.
   *
   * @param serializedPath Serialized path to file.
   * @return The modified Path.
   */
  public static Path deserializePath(String serializedPath) {
    final String jarPrefix = "jar:";
    final String filePrefix = "file://";
    String path = serializedPath;
    if (serializedPath.startsWith(jarPrefix)) {
      path = path.substring(jarPrefix.length());
    }
    if (serializedPath.startsWith(filePrefix)) {
      path = path.substring(filePrefix.length());
    }
    // Keep only one occurrence of "/" from the beginning if more than one exists.
    path = Paths.get(path).toString();
    int start = 0;
    while (start + 1 < path.length()
        && path.charAt(start) == '/'
        && path.charAt(start + 1) == '/') {
      start++;
    }
    return Paths.get(path.substring(start));
  }
}
