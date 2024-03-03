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
import static java.util.stream.Collectors.mapping;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.google.common.collect.ImmutableList;
import edu.ucr.cs.riple.injector.changes.ASTChange;
import edu.ucr.cs.riple.injector.changes.AddAnnotation;
import edu.ucr.cs.riple.injector.changes.AnnotationChange;
import edu.ucr.cs.riple.injector.changes.ChangeVisitor;
import edu.ucr.cs.riple.injector.changes.RemoveAnnotation;
import edu.ucr.cs.riple.injector.changes.TypeUseAnnotationChange;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.modifications.Modification;
import edu.ucr.cs.riple.injector.offsets.FileOffsetStore;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
          changeList = filterChange(changeList);
          combineTypeArgumentIndices(changeList);
          CompilationUnit tree;
          if (path == null || path.toString().equals("null")) {
            return;
          }
          try {
            tree = StaticJavaParser.parse(path);
          } catch (Exception exception) {
            System.err.println("Parse error on: " + path + " " + exception.getClass());
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
          String contentBefore = null;
          try {
            contentBefore = Files.readString(path);
            FileOffsetStore offsetStore = printer.write();
            offsets.add(offsetStore);
            StaticJavaParser.parse(path);
          } catch (Exception e) {
            System.out.println("Error happened on: " + path);
            try {
              Path bakPath = Paths.get(path.toString() + ".bak");
              Files.createFile(bakPath);
              if (contentBefore == null) {
                return;
              }
              Files.write(bakPath, contentBefore.getBytes(Charset.defaultCharset()));
              System.out.println("Applying changes:");
              changeList.forEach(System.out::println);
            } catch (Exception ex) {
              throw new RuntimeException(ex);
            }
            throw new RuntimeException(e);
          }
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

  public List<ASTChange> filterChange(List<ASTChange> changes) {
    //    Set<String> polyMethods =
    //        changes.stream()
    //            .filter(change -> change().isOnPolyMethod())
    //            .map(change -> change.getLocation().toPolyMethod().method)
    //            .collect(Collectors.toSet());
    //    return changes.stream()
    //        .filter(
    //            astChange -> {
    //              if (!astChange.getLocation().isOnParameter()) {
    //                return true;
    //              }
    //              return !polyMethods.contains(
    //                  astChange.getLocation().toParameter().enclosingMethod.method);
    //            })
    //        .collect(Collectors.toList());
    return changes;
  }

  public void combineTypeArgumentIndices(List<ASTChange> changes) {
    Map<Location, List<ImmutableList<ImmutableList<Integer>>>> map =
        changes.stream()
            .filter(astChange -> astChange instanceof TypeUseAnnotationChange)
            .collect(
                groupingBy(
                    ASTChange::getLocation,
                    mapping(
                        change -> ((TypeUseAnnotationChange) change).getTypeIndex(),
                        Collectors.toList())));
    changes.forEach(
        astChange -> {
          if (!(astChange instanceof TypeUseAnnotationChange)) {
            return;
          }
          List<ImmutableList<ImmutableList<Integer>>> typeIndices =
              map.get(astChange.getLocation());
          ImmutableList.Builder<ImmutableList<Integer>> builder = ImmutableList.builder();
          typeIndices.forEach(builder::addAll);
          ((TypeUseAnnotationChange) astChange).typeIndex = builder.build();
        });
  }
}
