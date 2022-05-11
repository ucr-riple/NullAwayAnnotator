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
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.printer.DefaultPrettyPrinter;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import me.tongfei.progressbar.ProgressBar;

public class Machine {

  private final List<WorkList> workLists;
  private final Injector.MODE mode;
  private final DefaultPrettyPrinter printer;
  private final boolean keep;
  private final int total;
  private int processed = 0;

  public Machine(List<WorkList> workLists, Injector.MODE mode, boolean keep) {
    this.workLists = workLists;
    this.mode = mode;
    this.keep = keep;
    this.printer = new DefaultPrettyPrinter(new DefaultPrinterConfiguration());
    AtomicInteger sum = new AtomicInteger();
    workLists.forEach(workList -> sum.addAndGet(workList.getLocations().size()));
    this.total = sum.get();
  }

  private void overWriteToFile(CompilationUnit changed, String uri) {
    if (mode.equals(Injector.MODE.TEST)) {
      uri = uri.replace("src", "out");
    }
    String pathToFileDirectory = uri.substring(0, uri.lastIndexOf("/"));
    try {
      Files.createDirectories(Paths.get(pathToFileDirectory + "/"));
      FileWriter writer = new FileWriter(uri);
      String toWrite = keep ? LexicalPreservingPrinter.print(changed) : printer.print(changed);
      writer.write(toWrite);
      writer.flush();
      writer.close();
    } catch (IOException e) {
      throw new RuntimeException("Something terrible happened.");
    }
  }

  public Integer start() {
    ProgressBar pb = Helper.createProgressBar("Injector", total);
    for (WorkList workList : workLists) {
      CompilationUnit tree;
      try {
        CompilationUnit tmp = StaticJavaParser.parse(new File(workList.getUri()));
        tree = keep ? LexicalPreservingPrinter.setup(tmp) : tmp;
      } catch (FileNotFoundException exception) {
        continue;
      }
      for (Change location : workList.getLocations()) {
        try {
          if (Injector.LOG) {
            pb.step();
          }
          if (applyChange(tree, location)) {
            processed++;
          }
        } catch (Exception ignored) {
        }
      }
      overWriteToFile(tree, workList.getUri());
    }
    if (Injector.LOG) {
      pb.stepTo(total);
    }
    pb.close();
    return processed;
  }

  private boolean applyChange(CompilationUnit tree, Change change) {
    boolean success = false;
    NodeList<BodyDeclaration<?>> clazz =
        Helper.getClassOrInterfaceOrEnumDeclarationMembersByFlatName(tree, change.clazz);
    if (clazz == null) {
      return false;
    }
    switch (change.kind) {
      case "FIELD":
        success = applyClassField(clazz, change);
        break;
      case "METHOD":
        success = applyMethodReturn(clazz, change);
        break;
      case "PARAMETER":
        success = applyMethodParam(clazz, change);
        break;
    }
    if (success) {
      if (Helper.getPackageName(change.annotation) != null) {
        ImportDeclaration importDeclaration =
            StaticJavaParser.parseImport("import " + change.annotation + ";");
        if (treeRequiresImportDeclaration(tree, importDeclaration, change.annotation)) {
          tree.getImports().addFirst(importDeclaration);
        }
      }
    }
    return success;
  }

  private boolean treeRequiresImportDeclaration(
      CompilationUnit tree, ImportDeclaration importDeclaration, String annotation) {
    if (tree.getImports().contains(importDeclaration)) {
      return false;
    }
    return tree.getImports()
        .stream()
        .noneMatch(
            impDecl ->
                Helper.simpleName(impDecl.getNameAsString()).equals(Helper.simpleName(annotation)));
  }

  private static void applyAnnotation(
      NodeWithAnnotations<?> node, String annotName, boolean inject) {
    final String annotSimpleName = Helper.simpleName(annotName);
    NodeList<AnnotationExpr> annots = node.getAnnotations();
    boolean exists =
        annots
            .stream()
            .anyMatch(
                annot -> {
                  String thisAnnotName = annot.getNameAsString();
                  return thisAnnotName.equals(annotName) || thisAnnotName.equals(annotSimpleName);
                });
    if (inject && !exists) {
      node.addMarkerAnnotation(annotSimpleName);
    }
    if (!inject) {
      annots.removeIf(
          annot -> {
            String thisAnnotName = annot.getNameAsString();
            return thisAnnotName.equals(annotName) || thisAnnotName.equals(annotSimpleName);
          });
    }
  }

  private boolean applyMethodParam(NodeList<BodyDeclaration<?>> members, Change location) {
    final boolean[] success = {false};
    members.forEach(
        bodyDeclaration ->
            bodyDeclaration.ifCallableDeclaration(
                callableDeclaration -> {
                  if (Helper.matchesCallableSignature(callableDeclaration, location.method)) {
                    for (Object p : callableDeclaration.getParameters()) {
                      if (p instanceof Parameter) {
                        Parameter param = (Parameter) p;
                        if (param.getName().toString().equals(location.variable)) {
                          applyAnnotation(
                              param, location.annotation, Boolean.parseBoolean(location.inject));
                          success[0] = true;
                        }
                      }
                    }
                  }
                }));
    return success[0];
  }

  private boolean applyMethodReturn(NodeList<BodyDeclaration<?>> members, Change location) {
    final boolean[] success = {false};
    members.forEach(
        bodyDeclaration ->
            bodyDeclaration.ifCallableDeclaration(
                callableDeclaration -> {
                  if (Helper.matchesCallableSignature(callableDeclaration, location.method)) {
                    applyAnnotation(
                        callableDeclaration,
                        location.annotation,
                        Boolean.parseBoolean(location.inject));
                    success[0] = true;
                  }
                }));
    return success[0];
  }

  private boolean applyClassField(NodeList<BodyDeclaration<?>> members, Change location) {
    final boolean[] success = {false};
    members.forEach(
        bodyDeclaration ->
            bodyDeclaration.ifFieldDeclaration(
                fieldDeclaration -> {
                  NodeList<VariableDeclarator> vars =
                      fieldDeclaration.asFieldDeclaration().getVariables();
                  for (VariableDeclarator v : vars) {
                    if (v.getName().toString().equals(location.variable)) {
                      applyAnnotation(
                          fieldDeclaration,
                          location.annotation,
                          Boolean.parseBoolean(location.inject));
                      success[0] = true;
                      break;
                    }
                  }
                }));

    return success[0];
  }
}
