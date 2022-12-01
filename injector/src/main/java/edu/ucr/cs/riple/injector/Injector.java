/*
 * Copyright (c) 2022 University of California, Riverside.
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

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import edu.ucr.cs.riple.injector.changes.AddAnnotation;
import edu.ucr.cs.riple.injector.changes.Change;
import edu.ucr.cs.riple.injector.changes.RemoveAnnotation;
import edu.ucr.cs.riple.injector.modifications.Modification;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Injector main class which can add / remove annotations. */
public class Injector {

  /**
   * Starts applying the requested changes.
   *
   * @param changes Set of changes.
   */
  public <T extends Change> void start(Set<T> changes) {
    // Start method does not support addition and deletion on same element. Should be split into
    // call for addition and deletion separately.
    Map<String, List<Change>> map = new HashMap<>();
    changes.forEach(
        change -> {
          if (map.containsKey(change.location.uri)) {
            map.get(change.location.uri).add(change);
          } else {
            List<Change> newList = new ArrayList<>();
            newList.add(change);
            map.put(change.location.uri, newList);
          }
        });
    map.forEach(
        (uri, changeList) -> {
          CompilationUnit tree;
          try {
            tree = LexicalPreservingPrinter.setup(StaticJavaParser.parse(new File(uri)));
          } catch (FileNotFoundException exception) {
            return;
          }
          Set<Modification> modifications = new HashSet<>();
          Set<ImportDeclaration> imports = new HashSet<>();
          for (Change change : changeList) {
            try {
              Modification modification = change.translate(tree);
              if (modification != null) {
                modifications.add(modification);
                if (change instanceof AddAnnotation) {
                  if (Helper.getPackageName(change.annotation) != null) {
                    ImportDeclaration importDeclaration =
                        StaticJavaParser.parseImport("import " + change.annotation + ";");
                    if (treeRequiresImportDeclaration(tree, importDeclaration, change.annotation)) {
                      imports.add(importDeclaration);
                    }
                  }
                }
              }
            } catch (Exception ex) {
              System.err.println("Encountered Exception: " + ex);
            }
          }
          Printer printer = new Printer(Paths.get(uri));
          printer.applyModifications(modifications);
          printer.addImports(tree, imports);
          printer.write();
        });
  }

  /**
   * Checks if the modifying tree, requires an addition of the import declaration due to the latest
   * changes.
   *
   * @param tree Modifying compilation unit tree.
   * @param importDeclaration Import declaration to be added.
   * @param annotation Recent added annotation.
   * @return true, if tree requires the import declaration.
   */
  private boolean treeRequiresImportDeclaration(
      CompilationUnit tree, ImportDeclaration importDeclaration, String annotation) {
    if (tree.getImports().contains(importDeclaration)) {
      return false;
    }
    return tree.getImports().stream()
        .noneMatch(
            impDecl ->
                Helper.simpleName(impDecl.getNameAsString()).equals(Helper.simpleName(annotation)));
  }

  /**
   * Adds the given annotations.
   *
   * @param requests Given annotations.
   */
  public void addAnnotations(Set<AddAnnotation> requests) {
    this.start(requests);
  }

  /**
   * Deletes the given annotations.
   *
   * @param requests Given annotations.
   */
  public void removeAnnotations(Set<RemoveAnnotation> requests) {
    this.start(requests);
  }
}
