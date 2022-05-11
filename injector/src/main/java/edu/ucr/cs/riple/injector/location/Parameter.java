package edu.ucr.cs.riple.injector.location;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import edu.ucr.cs.riple.injector.Helper;
import org.json.simple.JSONObject;

public class Parameter extends Location {
  final String method;
  final String parameter;
  final int index;

  public Parameter(String clazz, String uri, String method, String parameter, int index) {
    super("PARAMETER", clazz, uri);
    this.method = method;
    this.parameter = parameter;
    this.index = index;
  }

  @Override
  public Location duplicate() {
    return new Parameter(clazz, uri, method, parameter, index);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void fillJsonInformation(JSONObject res) {
    res.put(KEYS.METHOD, method);
    res.put(KEYS.PARAMETER, parameter);
    res.put(KEYS.INDEX, index);
  }

  @Override
  protected boolean applyToMember(
      NodeList<BodyDeclaration<?>> members, String annotation, boolean inject) {
    final boolean[] success = {false};
    members.forEach(
        bodyDeclaration ->
            bodyDeclaration.ifCallableDeclaration(
                callableDeclaration -> {
                  if (Helper.matchesCallableSignature(callableDeclaration, method)) {
                    for (Object p : callableDeclaration.getParameters()) {
                      if (p instanceof com.github.javaparser.ast.body.Parameter) {
                        com.github.javaparser.ast.body.Parameter param =
                            (com.github.javaparser.ast.body.Parameter) p;
                        if (param.getName().toString().equals(parameter)) {
                          applyAnnotation(param, annotation, inject);
                          success[0] = true;
                        }
                      }
                    }
                  }
                }));
    return success[0];
  }
}
