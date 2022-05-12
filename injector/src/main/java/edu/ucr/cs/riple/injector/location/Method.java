package edu.ucr.cs.riple.injector.location;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import edu.ucr.cs.riple.injector.Helper;
import org.json.simple.JSONObject;

public class Method extends Location {
  final String method;

  public Method(String clazz, String uri, String method) {
    super(LocationType.METHOD, clazz, uri);
    this.method = method;
  }

  @Override
  public Location duplicate() {
    return new Method(clazz, uri, method);
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
}
