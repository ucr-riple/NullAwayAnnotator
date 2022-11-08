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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Applies the text modification instances to source file. Text modifications are applied according
 * to the target element {@link javax.lang.model.element.ElementKind}.
 */
public class Printer {

  /** Path to source file. */
  private final Path path;
  /** Lines of source file. */
  private final List<String> lines;

  public Printer(Path path) {
    this.path = path;

    try {
      lines = Files.readAllLines(path);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Applies the set of modification to source file.
   *
   * @param modifications Set of modifications.
   */
  public void applyModifications(Set<Modification> modifications) {
    // Modification are sorted to start from the last position, to make the changes ineffective to
    // current computed offsets.
    modifications.stream()
        .sorted(
            Comparator.comparingInt((Modification o) -> o.startPosition.line)
                .thenComparingInt(o -> o.startPosition.column)
                // To make the tests produce deterministic results.
                .thenComparing(o -> o.content)
                .reversed())
        .forEach(modification -> modification.visit(lines));
  }

  /**
   * Adds the set of import declarations to source file.
   *
   * @param tree Compilation unit tree, required to compute the offset of the import declarations.
   * @param imports Set of import declarations to be added.
   */
  public void addImports(CompilationUnit tree, Set<ImportDeclaration> imports) {
    if (imports.size() == 0) {
      return;
    }
    int line = findStartOffsetForImports(tree);
    imports.forEach(importDec -> lines.add(line, importDec.toString()));
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
   *   <li>Immediately before non-empty line
   * </ol>
   *
   * @param tree The compilation unit tree.
   * @return The offset where all the new import declaration statements should be inserted.
   */
  private int findStartOffsetForImports(CompilationUnit tree) {
    // Get position of last import is exists
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
        return range.get().begin.line;
      }
    }
    // No package exists, add import under copy right header if exists, otherwise on the first line.
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i).strip();
      if (line.equals("")) {
        continue;
      }
      // For copy rights surrounded with "/* **/"
      if (line.startsWith("/*")) {
        while (i < lines.size()) {
          String endLine = lines.get(i);
          if (endLine.contains("*/")) {
            return i + 1;
          }
          i++;
        }
      }
      // For copy rights starting with "//" on each line.
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

  /** Writes the updated lines into the source file. */
  public void write() {
    try {
      Files.write(path, lines);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
