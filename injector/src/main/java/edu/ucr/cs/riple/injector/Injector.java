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

import static java.util.stream.Collectors.groupingBy;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import edu.ucr.cs.riple.injector.changes.ASTChange;
import edu.ucr.cs.riple.injector.changes.AddAnnotation;
import edu.ucr.cs.riple.injector.changes.AnnotationChange;
import edu.ucr.cs.riple.injector.changes.ChangeVisitor;
import edu.ucr.cs.riple.injector.changes.RemoveAnnotation;
import edu.ucr.cs.riple.injector.modifications.Modification;
import edu.ucr.cs.riple.injector.offsets.FileOffsetStore;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/** Injector main class which can add / remove annotations. */
public class Injector {

  /**
   * Starts applying the requested changes.
   *
   * @param changes Set of changes.
   * @return Offset changes of source file.
   */
  public <T extends ASTChange> Set<FileOffsetStore> start(Set<T> changes) {
    // Start method does not support addition and deletion on same element. Should be split into
    // call for addition and deletion separately.
    Map<Path, List<ASTChange>> map =
        changes.stream().collect(groupingBy(change -> change.getLocation().path));
    Set<FileOffsetStore> offsets = new HashSet<>();
    map.forEach(
        (path, changeList) -> {
          CompilationUnit tree = parse(path);
          if (tree == null) {
            return;
          }
          ChangeVisitor visitor = new ChangeVisitor(tree);
          Set<Modification> modifications = new HashSet<>();
          Set<ImportDeclaration> imports = new HashSet<>();
          for (ASTChange change : changeList) {
            try {
              Modification modification = visitor.computeModification(change);
              if (modification != null) {
                modifications.add(modification);
                if (change instanceof AddAnnotation) {
                  String annotationFullName = ((AnnotationChange) change).annotationName.fullName;
                  if (Helper.getPackageName(annotationFullName) != null) {
                    ImportDeclaration importDeclaration =
                        StaticJavaParser.parseImport("import " + annotationFullName + ";");
                    if (treeRequiresImportDeclaration(
                        tree, importDeclaration, annotationFullName)) {
                      imports.add(importDeclaration);
                    }
                  }
                }
              }
            } catch (Exception ex) {
              System.err.println("Encountered Exception: " + ex);
            }
          }
          Printer printer = new Printer(path);
          printer.applyModifications(modifications);
          printer.addImports(tree, imports);
          FileOffsetStore offsetStore = printer.write();
          offsets.add(offsetStore);
        });
    return offsets;
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
   * @return Offset changes of source file.
   */
  public Set<FileOffsetStore> addAnnotations(Set<AddAnnotation> requests) {
    return this.start(requests);
  }

  /**
   * Deletes the given annotations.
   *
   * @param requests Given annotations.
   * @return Offset changes of source file.
   */
  public Set<FileOffsetStore> removeAnnotations(Set<RemoveAnnotation> requests) {
    return this.start(requests);
  }

  /**
   * Parses the given file into a compilation unit tree. If the file does not exist, returns null.
   * Can happen when the file is generated by the compiler.
   *
   * @param path Path to the file.
   * @return Compilation unit tree, if the file does not exist, returns null.
   */
  @Nullable
  public static CompilationUnit parse(@Nullable Path path) {
    if (path == null) {
      return null;
    }
    ParserConfiguration parserConfiguration = new ParserConfiguration();
    // Set parser configuration to Java 17.
    parserConfiguration.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
    StaticJavaParser.setConfiguration(parserConfiguration);
    try {
      return StaticJavaParser.parse(path);
    } catch (NoSuchFileException e) {
      return null;
    } catch (IOException e) {
      throw new RuntimeException("Error happened on parsing file at: " + path, e);
    }
  }
}
