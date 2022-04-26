package edu.ucr.cs.riple.core.metadata.field;

import edu.ucr.cs.riple.core.metadata.MetaData;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.injector.Location;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class FieldInitializationAnalysis extends MetaData<FieldInitializationNode> {

  public FieldInitializationAnalysis(Path path) {
    super(path);
  }

  @Override
  protected FieldInitializationNode addNodeByLine(String[] values) {
    return new FieldInitializationNode(Location.fromArrayInfo(values, "null"), values[6]);
  }

  public Set<Location> findInitializers(Set<Fix> uninitializedFields) {
    // Set does not have a get() method, instead we use map here which can find the element
    // efficiently.
    Map<String, Class> classes = new HashMap<>();
    uninitializedFields.forEach(
        fix ->
            findAllNodes(
                    candidate ->
                        candidate.field.equals(fix.variable)
                            && candidate.initializerLocation.clazz.equals(fix.clazz))
                .forEach(
                    node -> {
                      Class clazz =
                          new Class(node.initializerLocation.clazz, node.initializerLocation.uri);
                      classes.putIfAbsent(clazz.clazz, clazz);
                      classes.get(clazz.clazz).accept(node);
                    }));
    return classes
        .values()
        .stream()
        .map(Class::findInitializer)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }
}
