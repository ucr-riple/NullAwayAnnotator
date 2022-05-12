package edu.ucr.cs.riple.injector.location;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import java.util.HashSet;
import java.util.Set;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Field extends Location {
  final Set<String> variables;

  public Field(String clazz, String uri, Set<String> variables) {
    super(LocationType.FIELD, clazz, uri);
    this.variables = variables;
  }

  @Override
  public Location duplicate() {
    return new Field(clazz, uri, new HashSet<>(variables));
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void fillJsonInformation(JSONObject res) {
    JSONArray fields = new JSONArray();
    fields.addAll(variables);
    res.put(KEYS.VARIABLES, fields);
  }

  @Override
  protected boolean applyToMember(
      NodeList<BodyDeclaration<?>> clazz, String annotation, boolean inject) {
    final boolean[] success = {false};
    clazz.forEach(
        bodyDeclaration ->
            bodyDeclaration.ifFieldDeclaration(
                fieldDeclaration -> {
                  NodeList<VariableDeclarator> vars =
                      fieldDeclaration.asFieldDeclaration().getVariables();
                  for (VariableDeclarator v : vars) {
                    if (variables.contains(v.getName().toString())) {
                      applyAnnotation(fieldDeclaration, annotation, inject);
                      success[0] = true;
                      break;
                    }
                  }
                }));
    return success[0];
  }
}
