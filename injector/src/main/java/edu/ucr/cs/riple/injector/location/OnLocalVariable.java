package edu.ucr.cs.riple.injector.location;

import com.github.javaparser.Range;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.visitor.GenericVisitorWithDefaults;
import edu.ucr.cs.riple.injector.Helper;
import edu.ucr.cs.riple.injector.SignatureMatcher;
import edu.ucr.cs.riple.injector.changes.Change;
import edu.ucr.cs.riple.injector.modifications.Modification;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
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

  public static final TypeArgumentVisitor TYPE_ARGUMENT_VISITOR = new TypeArgumentVisitor();

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
    final AtomicReference<List<Modification>> ans = new AtomicReference<>();
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
                              varDeclaration
                                  .getVariables()
                                  .forEach(
                                      variableDeclarator -> {
                                        if (variableDeclarator
                                            .getName()
                                            .toString()
                                            .equals(varName)) {
                                          Set<Range> ranges =
                                              variableDeclarator
                                                  .getType()
                                                  .accept(TYPE_ARGUMENT_VISITOR, null);
                                        }
                                      });
                            });
                  }
                }));
    return ans.get().get(0);
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

  static class TypeArgumentVisitor extends GenericVisitorWithDefaults<Set<Range>, Void> {

    @Override
    public Set<Range> visit(PrimitiveType n, Void unused) {
      if (n.getRange().isEmpty()) {
        return Set.of();
      }
      return Set.of(n.getRange().get());
    }

    @Override
    public Set<Range> visit(ClassOrInterfaceType classOrInterfaceType, Void unused) {
      if (classOrInterfaceType.getRange().isEmpty()) {
        return Set.of();
      }
      Set<Range> result = new HashSet<>();
      result.add(classOrInterfaceType.getRange().get());
      if (classOrInterfaceType.getTypeArguments().isPresent()) {
        classOrInterfaceType
            .getTypeArguments()
            .get()
            .forEach(e -> result.addAll(e.accept(this, null)));
      }
      return result;
    }
  }
}
