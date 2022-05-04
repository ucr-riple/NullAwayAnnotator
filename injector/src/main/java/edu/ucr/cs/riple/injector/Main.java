package edu.ucr.cs.riple.injector;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import java.io.File;
import java.io.FileNotFoundException;

public class Main {
  public static void main(String[] args) throws FileNotFoundException {
    CompilationUnit tree =
        StaticJavaParser.parse(
            new File(
                "/Users/nima/Developer/NullAwayFixer/Projects/Test/src/main/java/injector/Main.java"));
    Node clazz = Helper.getClassOrInterfaceOrEnumDeclaration(tree, "injector.Main$1$1");
    System.out.println(clazz);
  }
}
