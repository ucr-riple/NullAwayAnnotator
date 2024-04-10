/*
 * Copyright (c) 2024 Nima Karimipour.
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

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Range;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.NodeWithRange;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.WildcardType;
import com.github.javaparser.ast.visitor.GenericVisitorWithDefaults;
import com.github.javaparser.ast.visitor.TreeVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class Main {

  static final AnnotationExpr UNTAINTED = new MarkerAnnotationExpr("RUntainted");
  static int TOP_LEVEL_COUNT = 0;
  static int TYPE_ARG_COUNT = 0;

  public static void main(String[] args) throws IOException {
    String directory = "/Users/nima/Developer/NullAwayAnnotator/sample";
    try (Stream<Path> paths = Files.walk(Paths.get(directory))) {
      paths.forEach(
          path -> {
            if (path.toString().endsWith(".java")) {
              try {
                StaticJavaParser.setConfiguration(
                    new ParserConfiguration()
                        .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17));
                CompilationUnit tree = StaticJavaParser.parse(path.toFile());
                TreeVisitor visitor =
                    new TreeVisitor() {
                      @Override
                      public void process(Node node) {
                        node.accept(new AnnotationVisitor(), null);
                      }
                    };
                visitor.visitBreadthFirst(tree);
              } catch (FileNotFoundException e) {
                System.out.println("Exception for : " + path + " " + e);
              }
            }
          });
    }
    System.out.println("Top-level: " + TOP_LEVEL_COUNT);
    System.out.println("Type arg: " + TYPE_ARG_COUNT);
  }

  static class AnnotationVisitor extends VoidVisitorAdapter<Void> {

    static final Set<Range> visitedRanges = new HashSet<>();

    @Override
    public void visit(final MethodDeclaration n, final Void arg) {
      checkForAnnotation(n);
    }

    @Override
    public void visit(final FieldDeclaration n, final Void arg) {
      checkForAnnotation(n);
    }

    @Override
    public void visit(final AnnotationMemberDeclaration n, final Void arg) {
      checkForAnnotation(n);
    }

    @Override
    public void visit(final Parameter n, final Void arg) {
      checkForAnnotation(n);
    }

    @Override
    public void visit(final VariableDeclarationExpr n, final Void arg) {
      checkForAnnotation(n);
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Void arg) {
      Stream.concat(n.getExtendedTypes().stream(), n.getImplementedTypes().stream())
          .forEach(this::checkForAnnotation);
    }

    @Override
    public void visit(ObjectCreationExpr n, Void arg) {
      checkForAnnotation(n.getType());
    }

    private <T extends NodeWithAnnotations<?> & NodeWithRange<?>> void checkForAnnotation(T node) {
      if (node.getRange().isEmpty()) {
        return;
      }
      if (visitedRanges.contains(node.getRange().get())) {
        return;
      }
      visitedRanges.add(node.getRange().get());
      Type type = Helper.getType(node);
      if (type == null) {
        return;
      }
      boolean annotOnTopLevel =
          Helper.isAnnotatedWith(node, UNTAINTED) || Helper.isAnnotatedWith(type, UNTAINTED);
      int count = annotOnTopLevel ? 1 : 0;
      count += type.accept(new AnnotationCounter(), null);
      if (count > 0) {
        String n = node.toString();
        if (node instanceof MethodDeclaration) {
          n = ((MethodDeclaration) node).getDeclarationAsString();
        }
        System.out.println(
            n
                + " - "
                + "top-level "
                + (annotOnTopLevel ? "1" : "0")
                + " - type arg "
                + (annotOnTopLevel ? count - 1 : count));
        if (annotOnTopLevel) {
          TOP_LEVEL_COUNT++;
          TYPE_ARG_COUNT += count - 1;
        } else {
          TYPE_ARG_COUNT += count;
        }
      }
    }
  }

  static class AnnotationCounter extends GenericVisitorWithDefaults<Integer, Void> {

    @Override
    public Integer visit(ClassOrInterfaceType type, Void unused) {
      AtomicInteger start = new AtomicInteger();
      if (type.getTypeArguments().isEmpty()) {
        if (hasAnnotation(type)) {
          start.incrementAndGet();
        }
      }
      if (type.getTypeArguments().isEmpty()) {
        return start.get();
      }
      type.getTypeArguments()
          .get()
          .forEach(
              typeArg -> {
                Integer i = typeArg.accept(AnnotationCounter.this, unused);
                if (i != null) {
                  start.addAndGet(i);
                }
              });
      return start.get();
    }

    @Override
    public Integer visit(ArrayType type, Void unused) {
      return type.getComponentType().accept(this, unused);
    }

    @Override
    public Integer visit(WildcardType type, Void unused) {
      return hasAnnotation(type) ? 1 : 0;
    }

    @Override
    public Integer defaultAction(Node n, Void arg) {
      return 0;
    }
  }

  private static boolean hasAnnotation(NodeWithAnnotations<?> node) {
    Type type = Helper.getType(node);
    if (type == null) {
      return false;
    }
    return Helper.isAnnotatedWith(type, UNTAINTED) || Helper.isAnnotatedWith(node, UNTAINTED);
  }
}
