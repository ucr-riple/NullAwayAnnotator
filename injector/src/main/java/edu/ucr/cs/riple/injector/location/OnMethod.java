package edu.ucr.cs.riple.injector.location;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import edu.ucr.cs.riple.injector.Helper;
import java.util.Objects;
import java.util.function.Consumer;
import org.json.simple.JSONObject;

public class OnMethod extends Location {
  public final String method;

  public OnMethod(String uri, String clazz, String method) {
    super(LocationType.METHOD, uri, clazz);
    this.method = method;
  }

  @Override
  public Location duplicate() {
    return new OnMethod(clazz, uri, method);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void fillJsonInformation(JSONObject res) {
    res.put(KEYS.METHOD, method);
  }

  @Override
  protected boolean applyToMember(
      NodeList<BodyDeclaration<?>> clazz, String annotation, boolean inject) {
    final boolean[] success = {false};
    clazz.forEach(
        bodyDeclaration ->
            bodyDeclaration.ifCallableDeclaration(
                callableDeclaration -> {
                  if (Helper.matchesCallableSignature(callableDeclaration, method)) {
                    applyAnnotation(callableDeclaration, annotation, inject);
                    success[0] = true;
                  }
                }));
    return success[0];
  }

  @Override
  public void ifMethod(Consumer<OnMethod> consumer) {
    consumer.accept(this);
  }

  @Override
  public boolean isOnMethod() {
    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof OnMethod)) return false;
    OnMethod other = (OnMethod) o;
    return super.equals(other) && method.equals(other.method);
  }

  @Override
  public int hashCode() {
    return Objects.hash(method);
  }
}
