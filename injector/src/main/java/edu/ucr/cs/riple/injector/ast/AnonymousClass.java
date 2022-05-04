package edu.ucr.cs.riple.injector.ast;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.SimpleName;
import java.util.stream.Collectors;

public class AnonymousClass extends ClassOrInterfaceDeclaration {

  final NodeList<BodyDeclaration<?>> actualMembers;

  private static NodeList<BodyDeclaration<?>> cloneNodeList(NodeList<BodyDeclaration<?>> list) {
    return new NodeList<>(list.stream().map(BodyDeclaration::clone).collect(Collectors.toList()));
  }

  public AnonymousClass(String simpleName, NodeList<BodyDeclaration<?>> actualMembers) {
    this(simpleName, cloneNodeList(actualMembers), actualMembers);
  }

  public AnonymousClass(
      String simpleName,
      NodeList<BodyDeclaration<?>> copied,
      NodeList<BodyDeclaration<?>> actualMembers) {
    super(
        null,
        new NodeList<>(),
        new NodeList<>(),
        false,
        new SimpleName(simpleName),
        new NodeList<>(),
        new NodeList<>(),
        new NodeList<>(),
        copied);
    this.actualMembers = actualMembers;
  }

  public NodeList<BodyDeclaration<?>> getActualMembers() {
    return actualMembers;
  }
}
