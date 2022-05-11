package edu.ucr.cs.riple.core.metadata.field;

import edu.ucr.cs.riple.core.FixType;
import edu.ucr.cs.riple.injector.Change;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class Class {
  final Set<Method> methods;
  final String clazz;
  final String uri;
  final Set<String> fields;

  public Class(String clazz, String uri) {
    this.methods = new HashSet<>();
    this.clazz = clazz;
    this.uri = uri;
    this.fields = new HashSet<>();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Class)) return false;
    Class other = (Class) o;
    return clazz.equals(other.clazz) && uri.equals(other.uri);
  }

  @Override
  public int hashCode() {
    return Objects.hash(clazz, uri);
  }

  public void accept(FieldInitializationNode fieldInitializationNode) {
    Optional<Method> optionalMethod =
        this.methods
            .stream()
            .filter(
                method -> method.signature.equals(fieldInitializationNode.initializerLocation.method))
            .findAny();
    if (optionalMethod.isPresent()) {
      optionalMethod.get().fields.add(fieldInitializationNode.field);
    } else {
      Method method = new Method(fieldInitializationNode.initializerLocation.method);
      method.fields.add(fieldInitializationNode.field);
      this.methods.add(method);
    }
  }

  public Change findInitializer() {
    Method maxMethod = null;
    int maxScore = 1;
    for (Method m : this.methods) {
      if (m.fields.size() > maxScore) {
        maxScore = m.fields.size();
        maxMethod = m;
      }
    }
    return maxMethod == null
        ? null
        : new Change(
            "null", maxMethod.signature, "null", FixType.METHOD.name, this.clazz, this.uri, "true");
  }
}
