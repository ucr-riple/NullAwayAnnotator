package edu.ucr.cs.riple.core.metadata.field;

import edu.ucr.cs.riple.core.metadata.MetaData;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.injector.location.OnMethod;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Detects initializer methods in source code. Reads field initialization info serialized by
 * NullAway and uses a heuristic to detect them.
 */
public class FieldInitializationAnalysis extends MetaData<FieldInitializationNode> {

  /** Stores class field / method initialization status. */
  private static class Class {
    /** Set of initializer methods. */
    private final Set<InitializerMethod> methods;
    /** Fully qualified name of the class. */
    private final String clazz;
    /** URI to file where the class exists. */
    private final String uri;

    /**
     * Creates an instance.
     *
     * @param clazz Fully qualified name.
     * @param uri URI to the file where the class exists.
     */
    private Class(String clazz, String uri) {
      this.methods = new HashSet<>();
      this.clazz = clazz;
      this.uri = uri;
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

    /**
     * Visits a {@link FieldInitializationNode} instance and updates its internal state, it
     * processes the initializer method and stores its info with the initializing field.
     *
     * @param fieldInitializationNode FieldInitialization instance.
     */
    private void visit(FieldInitializationNode fieldInitializationNode) {
      // Check if initializer node belongs to this class.
      if (!fieldInitializationNode.getClassName().equals(clazz)) {
        return;
      }
      // Check if initializer method has been observed before.
      Optional<InitializerMethod> optionalMethod =
          this.methods.stream()
              .filter(
                  method -> method.signature.equals(fieldInitializationNode.getInitializerMethod()))
              .findAny();
      if (optionalMethod.isPresent()) {
        // Method has been observed before, add the field info.
        optionalMethod.get().fields.add(fieldInitializationNode.getFieldName());
      } else {
        // Method has not been observed before, create and add the field info.
        InitializerMethod method =
            new InitializerMethod(fieldInitializationNode.getInitializerMethod());
        method.fields.add(fieldInitializationNode.getFieldName());
        // Add to list of observed.
        this.methods.add(method);
      }
    }

    /**
     * Selects an initializer for this class, Each class can have at most one initializer, this
     * method will nominate the method with the most initialized number (more than 1) of
     * uninitialized fields.
     *
     * @return The nominated Initializer method.
     */
    private OnMethod findInitializer() {
      InitializerMethod maxMethod = null;
      int maxScore = 1; // Initializer score must be at least 1.
      for (InitializerMethod m : this.methods) {
        if (m.fields.size() > maxScore) {
          maxScore = m.fields.size();
          maxMethod = m;
        }
      }
      return maxMethod == null ? null : new OnMethod(uri, clazz, maxMethod.signature);
    }
  }

  /** Stores information for Initializer Methods. */
  private static class InitializerMethod {
    /** Methods signature. */
    private final String signature;
    /** Initialized field. */
    private final Set<String> fields;

    private InitializerMethod(String signature) {
      this.signature = signature;
      this.fields = new HashSet<>();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof InitializerMethod)) return false;
      InitializerMethod other = (InitializerMethod) o;
      return signature.equals(other.signature);
    }

    @Override
    public int hashCode() {
      return Objects.hash(signature);
    }
  }

  /**
   * Constructs an {@link FieldInitializationAnalysis} instance. After this call, all serialized
   * information from NullAway has been processed.
   *
   * @param path Path to field initialization info coming from NullAway.
   */
  public FieldInitializationAnalysis(Path path) {
    super(path);
  }

  @Override
  protected FieldInitializationNode addNodeByLine(String[] values) {
    Location location = Location.createLocationFromArrayInfo(values);
    return location.isOnMethod()
        ? new FieldInitializationNode(location.toMethod(), values[6])
        : null;
  }

  /**
   * Processes all uninitialized fields and method initializers and chooses the initializers with
   * the heuristic below:
   *
   * <ol>
   *   <li>It method must initialize at least more than one uninitialized field.
   *   <li>Each initialization, increments method's score by one.
   *   <li>Each class can have at most one initializer and the method with highest score will be
   *       selected.
   * </ol>
   *
   * @param uninitializedFields Set of uninitialized fields.
   * @return Location of initializers.
   */
  public Stream<OnMethod> findInitializers(Set<OnField> uninitializedFields) {
    // Set does not have a get() method, instead we use map here which can find the element
    // efficiently.
    Map<String, Class> classes = new HashMap<>();
    uninitializedFields.forEach(
        onField ->
            findNodesWithHashHint(
                    candidate ->
                        onField.isOnFieldWithName(candidate.getFieldName())
                            && candidate.getClassName().equals(onField.clazz),
                    FieldInitializationNode.hash(onField.clazz))
                .forEach(
                    node -> {
                      Class clazz = new Class(node.getClassName(), node.getURI());
                      classes.putIfAbsent(clazz.clazz, clazz);
                      classes.get(clazz.clazz).visit(node);
                    }));
    return classes.values().stream().map(Class::findInitializer).filter(Objects::nonNull);
  }
}
