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
import com.github.javaparser.printer.DefaultPrettyPrinter;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import edu.ucr.cs.riple.injector.changes.Change;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import me.tongfei.progressbar.ProgressBar;

public class Machine {

  private final List<WorkList> workLists;
  private final DefaultPrettyPrinter printer;
  private final boolean keep;
  private final int total;
  private int processed = 0;
  private final boolean log;

  public Machine(List<WorkList> workLists, boolean keep, boolean log) {
    this.workLists = workLists;
    this.keep = keep;
    this.log = log;
    this.printer = new DefaultPrettyPrinter(new DefaultPrinterConfiguration());
    AtomicInteger sum = new AtomicInteger();
    workLists.forEach(workList -> sum.addAndGet(workList.getChanges().size()));
    this.total = sum.get();
  }

  private void overWriteToFile(CompilationUnit changed, String uri) {
    Path path = Paths.get(uri);
    try (Writer writer =
        Files.newBufferedWriter(path.toFile().toPath(), Charset.defaultCharset())) {
      Files.createDirectories(path.getParent());
      String toWrite = keep ? LexicalPreservingPrinter.print(changed) : printer.print(changed);
      writer.write(toWrite);
      writer.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
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
      for (Change location : workList.getChanges()) {
        try {
          if (log) {
            pb.step();
          }
          if (applyChange(tree, location)) {
            processed++;
          }
        } catch (Exception ignored) {
          System.err.println("Encountered Exception: " + ignored);
        }
      }
      overWriteToFile(tree, workList.getUri());
    }
    if (log) {
      pb.stepTo(total);
    }
    pb.close();
    return processed;
  }

  private boolean applyChange(CompilationUnit tree, Change change) {
    boolean success = change.apply(tree);
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
    return tree.getImports().stream()
        .noneMatch(
            impDecl ->
                Helper.simpleName(impDecl.getNameAsString()).equals(Helper.simpleName(annotation)));
  }
}
