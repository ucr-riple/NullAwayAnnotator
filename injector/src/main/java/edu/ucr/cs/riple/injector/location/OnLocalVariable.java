package edu.ucr.cs.riple.injector.location;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import edu.ucr.cs.riple.injector.Helper;
import edu.ucr.cs.riple.injector.SignatureMatcher;
import edu.ucr.cs.riple.injector.changes.Change;
import edu.ucr.cs.riple.injector.modifications.Modification;
import edu.ucr.cs.riple.injector.modifications.MultiPositionModification;
import edu.ucr.cs.riple.injector.modifications.TypeChangeVisitor;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.json.simple.JSONObject;

/**
 * Represents a location for local variable element. This location is used to apply changes to a
 * local variable.
 */
public class OnLocalVariable extends Location {

  /** Enclosing method signature of the target local variable. */
  public final String encMethod;
  /**
   * Matcher for the method signature. Method signature is given as a string, this matcher is used
   * to match the target.
   */
  public final SignatureMatcher matcher;
  /** Name of the local variable. */
  public final String varName;

  /** Visitor for applying changes on the internal structure of the target element's type */
  public static final TypeChangeVisitor TYPE_CHANGE_VISITOR = new TypeChangeVisitor();

  public OnLocalVariable(Path path, String clazz, String encMethod, String varName) {
    super(LocationKind.LOCAL_VARIABLE, path, clazz);
    this.encMethod = encMethod;
    this.matcher = new SignatureMatcher(encMethod);
    this.varName = varName;
  }

  public OnLocalVariable(String path, String clazz, String encMethod, String varName) {
    this(Helper.deserializePath(path), clazz, encMethod, varName);
  }

  @Override
  protected void fillJsonInformation(JSONObject res) {
    throw new RuntimeException("Not implemented yet");
  }

  @Override
  protected Modification applyToMember(NodeList<BodyDeclaration<?>> members, Change change) {
    final AtomicReference<Modification> ans = new AtomicReference<>();
    members.forEach(
        bodyDeclaration ->
            bodyDeclaration.ifCallableDeclaration(
                callableDeclaration -> {
                  if (ans.get() != null) {
                    // already found the member.
                    return;
                  }
                  if (matcher.matchesCallableDeclaration(callableDeclaration)) {
                    callableDeclaration.stream()
                        .forEach(
                            node -> {
                              if (!(node instanceof VariableDeclarationExpr)) {
                                return;
                              }
                              VariableDeclarationExpr varDeclaration =
                                  (VariableDeclarationExpr) node;
                              if (varDeclaration.getRange().isEmpty()) {
                                return;
                              }
                              varDeclaration
                                  .getVariables()
                                  .forEach(
                                      variableDeclarator -> {
                                        if (variableDeclarator
                                            .getName()
                                            .toString()
                                            .equals(varName)) {
                                          // Located the variable.
                                          Set<Modification> modifications = new HashSet<>();
                                          // Process the declaration statement.
                                          modifications.add(
                                              change.visit(
                                                  varDeclaration, varDeclaration.getRange().get()));
                                          // Process the declarator type arguments.
                                          modifications.addAll(
                                              variableDeclarator
                                                  .getType()
                                                  .accept(TYPE_CHANGE_VISITOR, change));
                                          ans.set(new MultiPositionModification(modifications));
                                        }
                                      });
                            });
                  }
                }));
    return ans.get();
  }

  @Override
  public void ifLocalVariable(Consumer<OnLocalVariable> consumer) {
    consumer.accept(this);
  }

  @Override
  public boolean isOnLocalVariable() {
    return true;
  }

  @Override
  public boolean equals(Object o) {
    boolean ans = super.equals(o);
    if (!ans) {
      return false;
    }
    OnLocalVariable that = (OnLocalVariable) o;
    return this.encMethod.equals(that.encMethod) && this.varName.equals(that.varName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), encMethod, varName);
  }

  @Override
  public String toString() {
    return "OnLocalVariable{"
        + "encMethod='"
        + encMethod
        + '\''
        + ", clazz='"
        + clazz
        + '\''
        + "varName='"
        + varName
        + '}';
  }
}
