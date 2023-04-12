package edu.ucr.cs.riple.injector.location;

import com.github.javaparser.ast.body.CallableDeclaration;
import edu.ucr.cs.riple.injector.Helper;
import edu.ucr.cs.riple.injector.SignatureMatcher;
import edu.ucr.cs.riple.injector.visitors.LocationVisitor;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;
import org.json.simple.JSONObject;

/**
 * Represents a location for local variable element. This location is used to apply changes to a
 * local variable.
 *
 * <p>Annotations on local variables will be applied on the declaration and all its type arguments
 * if exists. For instance: {@code Foo<Bar, Map<Bar, Bar>> baz}
 *
 * <p>Will be annotated as: {@code @Annot Foo<@Annot Bar, @Annot Map<@Annot Bar, @Annot Bar>> baz}
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

  public OnLocalVariable(Path path, String clazz, String encMethod, String varName) {
    super(LocationKind.LOCAL_VARIABLE, path, clazz);
    this.encMethod = encMethod;
    this.matcher = new SignatureMatcher(encMethod);
    this.varName = varName;
  }

  public OnLocalVariable(String path, String clazz, String encMethod, String varName) {
    this(Helper.deserializePath(path), clazz, encMethod, varName);
  }

  public OnLocalVariable(JSONObject json) {
    super(LocationKind.LOCAL_VARIABLE, json);
    this.encMethod = (String) json.get("method");
    this.matcher = new SignatureMatcher(encMethod);
    this.varName = (String) json.get("varName");
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
  public <R, P> R accept(LocationVisitor<R, P> v, P p) {
    return v.visitLocalVariable(this, p);
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

  /**
   * Checks if the given method matches the method signature of this location.
   *
   * @param method method to check.
   * @return true, if the given method matches the method signature of this location.
   */
  public boolean matchesCallableDeclaration(CallableDeclaration<?> method) {
    return this.matcher.matchesCallableDeclaration(method);
  }
}
