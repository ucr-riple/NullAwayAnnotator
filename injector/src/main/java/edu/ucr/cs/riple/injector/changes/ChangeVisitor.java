/*
 * Copyright (c) 2023 University of California, Riverside.
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

package edu.ucr.cs.riple.injector.changes;

import static edu.ucr.cs.riple.injector.location.OnClass.isAnonymousClassFlatName;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.utils.Pair;
import edu.ucr.cs.riple.injector.Helper;
import edu.ucr.cs.riple.injector.exceptions.TargetClassNotFound;
import edu.ucr.cs.riple.injector.location.LocationVisitor;
import edu.ucr.cs.riple.injector.location.OnClass;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.injector.location.OnLocalVariable;
import edu.ucr.cs.riple.injector.location.OnMethod;
import edu.ucr.cs.riple.injector.location.OnParameter;
import edu.ucr.cs.riple.injector.modifications.Modification;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;

/**
 * A visitor for computing the required {@link Modification} to a compilation unit on a specified
 * location for the requested change.
 */
public class ChangeVisitor
    implements LocationVisitor<Modification, Pair<NodeList<BodyDeclaration<?>>, ASTChange>> {

  /** Compilation unit which the changes will be applied. */
  private final CompilationUnit cu;

  public ChangeVisitor(CompilationUnit cu) {
    this.cu = cu;
  }

  @Override
  @Nullable
  public Modification visitMethod(
      OnMethod onMethod, Pair<NodeList<BodyDeclaration<?>>, ASTChange> pair) {
    final AtomicReference<Modification> ans = new AtomicReference<>();
    final NodeList<BodyDeclaration<?>> members = pair.a;
    final ASTChange change = pair.b;
    members.forEach(
        bodyDeclaration ->
            bodyDeclaration.ifCallableDeclaration(
                callableDeclaration -> {
                  if (ans.get() != null) {
                    // already found the member.
                    return;
                  }
                  if (onMethod.matchesCallableDeclaration(callableDeclaration)) {
                    ans.set(change.computeTextModificationOn(callableDeclaration));
                  }
                }));
    if (ans.get() == null) {
      members.forEach(
          bodyDeclaration ->
              bodyDeclaration.ifAnnotationMemberDeclaration(
                  annotationMemberDeclaration -> {
                    if (annotationMemberDeclaration
                        .getNameAsString()
                        .equals(Helper.extractCallableName(onMethod.method))) {
                      ans.set(change.computeTextModificationOn(annotationMemberDeclaration));
                    }
                  }));
    }
    return ans.get();
  }

  @Override
  @Nullable
  public Modification visitField(
      OnField onField, Pair<NodeList<BodyDeclaration<?>>, ASTChange> pair) {
    final AtomicReference<Modification> ans = new AtomicReference<>();
    final NodeList<BodyDeclaration<?>> members = pair.a;
    final ASTChange change = pair.b;
    members.forEach(
        bodyDeclaration ->
            bodyDeclaration.ifFieldDeclaration(
                fieldDeclaration -> {
                  if (ans.get() != null) {
                    // already found the member.
                    return;
                  }
                  NodeList<VariableDeclarator> vars =
                      fieldDeclaration.asFieldDeclaration().getVariables();
                  for (VariableDeclarator v : vars) {
                    if (onField.variables.contains(v.getName().toString())) {
                      ans.set(change.computeTextModificationOn(fieldDeclaration));
                      break;
                    }
                  }
                }));
    return ans.get();
  }

  @Override
  @Nullable
  public Modification visitParameter(
      OnParameter onParameter, Pair<NodeList<BodyDeclaration<?>>, ASTChange> pair) {
    final AtomicReference<Modification> ans = new AtomicReference<>();
    final NodeList<BodyDeclaration<?>> members = pair.a;
    final ASTChange change = pair.b;
    members.forEach(
        bodyDeclaration ->
            bodyDeclaration.ifCallableDeclaration(
                callableDeclaration -> {
                  if (ans.get() != null) {
                    // already found the member.
                    return;
                  }
                  if (onParameter.enclosingMethod.matchesCallableDeclaration(callableDeclaration)) {
                    NodeList<?> params = callableDeclaration.getParameters();
                    if (onParameter.index < params.size()) {
                      if (params.get(onParameter.index) != null) {
                        Node param = params.get(onParameter.index);
                        if (param instanceof Parameter) {
                          ans.set(change.computeTextModificationOn((Parameter) param));
                        }
                      }
                    }
                  }
                }));
    return ans.get();
  }

  @Override
  @Nullable
  public Modification visitClass(
      OnClass onClass, Pair<NodeList<BodyDeclaration<?>>, ASTChange> pair) {
    final NodeList<BodyDeclaration<?>> members = pair.a;
    final ASTChange change = pair.b;
    if (isAnonymousClassFlatName(change.getLocation().clazz)) {
      return null;
    }
    // Get the enclosing class of the members
    Optional<Node> optionalClass = members.getParentNode();
    if (optionalClass.isEmpty() || !(optionalClass.get() instanceof BodyDeclaration<?>)) {
      return null;
    }
    return change.computeTextModificationOn(((BodyDeclaration<?>) optionalClass.get()));
  }

  @Override
  public Modification visitLocalVariable(
      OnLocalVariable onLocalVariable, Pair<NodeList<BodyDeclaration<?>>, ASTChange> pair) {
    final NodeList<BodyDeclaration<?>> members = pair.a;
    final ASTChange change = pair.b;
    for (BodyDeclaration<?> member : members) {
      if (member instanceof CallableDeclaration<?>) {
        CallableDeclaration<?> callableDeclaration = (CallableDeclaration<?>) member;
        if (onLocalVariable.encMethod.matchesCallableDeclaration(callableDeclaration)) {
          // Find variable declaration in the callable declaration with the variable name.
          VariableDeclarationExpr variableDeclarationExpr =
              Helper.locateVariableDeclarationExpr(callableDeclaration, onLocalVariable.varName);
          if (variableDeclarationExpr == null) {
            return null;
          }
          for (VariableDeclarator variableDeclarator : variableDeclarationExpr.getVariables()) {
            if (variableDeclarator.getName().toString().equals(onLocalVariable.varName)) {
              // Located the variable.
              return change.computeTextModificationOn(variableDeclarationExpr);
            }
          }
        }
      }
    }
    return null;
  }

  /**
   * Computes the required {@link Modification} that should be applied to the compilation unit for
   * the given change.
   *
   * @param change the change to apply.
   * @return the modification that should be applied.
   */
  @Nullable
  public Modification computeModification(ASTChange change) {
    NodeList<BodyDeclaration<?>> members;
    try {
      members = Helper.getTypeDeclarationMembersByFlatName(cu, change.getLocation().clazz);
      if (members == null) {
        return null;
      }
      return change.getLocation().accept(this, new Pair<>(members, change));
    } catch (TargetClassNotFound notFound) {
      System.err.println(notFound.getMessage());
      return null;
    }
  }
}
