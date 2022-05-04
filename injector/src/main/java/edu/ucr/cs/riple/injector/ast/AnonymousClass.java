package edu.ucr.cs.riple.injector.ast;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;

public class AnonymousClass extends TypeDeclaration<AnonymousClass> {

  public AnonymousClass(String simpleName, NodeList<BodyDeclaration<?>> members) {
    super(new NodeList<>(), new NodeList<>(), new SimpleName(simpleName), members);
  }

  @Override
  public ResolvedReferenceTypeDeclaration resolve() {
    throw new IllegalStateException(
        "edu.ucr.cs.riple.injector.ast.AnonymousClass does not currently support resolve()");
  }

  @Override
  public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
    throw new IllegalStateException(
        "edu.ucr.cs.riple.injector.ast.AnonymousClass does not currently support accept(GenericVisitor<R, A> v, A arg)");
  }

  @Override
  public <A> void accept(VoidVisitor<A> v, A arg) {
    throw new IllegalStateException(
        "edu.ucr.cs.riple.injector.ast.AnonymousClass does not currently support accept(GenericVisitor<R, A> v, A arg)");
  }
}
