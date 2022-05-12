package edu.ucr.cs.riple.injector.location;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class OnField extends Location {
  public final Set<String> variables;

  public OnField(String uri, String clazz, Set<String> variables) {
    super(LocationType.FIELD, uri, clazz);
    this.variables = variables;
  }

  @Override
  public Location duplicate() {
    return new OnField(clazz, uri, new HashSet<>(variables));
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

  @Override
  public void ifField(Consumer<OnField> consumer) {
    consumer.accept(this);
  }

  @Override
  public boolean isOnField() {
    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof OnField)) return false;
    if (!super.equals(o)) return false;
    OnField other = (OnField) o;
    return super.equals(other) && !Collections.disjoint(variables, other.variables);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), variables);
  }
}
