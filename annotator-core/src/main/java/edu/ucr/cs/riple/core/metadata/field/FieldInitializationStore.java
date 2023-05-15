package edu.ucr.cs.riple.core.metadata.field;

import com.google.common.base.Preconditions;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.metadata.Registry;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.injector.location.OnMethod;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Detects initializer methods in source code. Reads field initialization info serialized by
 * NullAway and uses a heuristic to detect them.
 */
public class FieldInitializationStore extends Registry<FieldInitializationNode> {

  /**
   * Output file name. It contains information about initializations of fields via methods. On each
   * row, an initializer method along the initialized field is written. A method is considered to be
   * an initializer for a filed, if the method writes a {@code @Nonnull} value to the field and
   * leaves it {@code @Nonnull} at exit.
   */
  public static final String FILE_NAME = "field_init.tsv";

  /**
   * Constructs an {@link FieldInitializationStore} instance. After this call, all serialized
   * information from NullAway has been processed.
   *
   * @param config Annotator config.
   */
  public FieldInitializationStore(Config config) {
    super(config, config.target.dir.resolve(FILE_NAME));
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
  public Set<OnMethod> findInitializers(Set<OnField> uninitializedFields) {
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
                      Class clazz = new Class(node.getClassName(), node.getPath());
                      classes.putIfAbsent(clazz.clazz, clazz);
                      classes.get(clazz.clazz).visit(node);
                    }));
    return classes.values().stream()
        .map(Class::findInitializer)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  @Override
  protected Builder<FieldInitializationNode> getBuilder() {
    return values -> {
      Location location = Location.createLocationFromArrayInfo(values);
      Preconditions.checkNotNull(
          location, "Field Location cannot be null: " + Arrays.toString(values));
      return location.isOnMethod()
          ? new FieldInitializationNode(location.toMethod(), values[6])
          : null;
    };
  }

  /** Stores class field / method initialization status. */
  @SuppressWarnings("JavaLangClash")
  private static class Class {
    /**
     * HashMap of initializer methods. Used HashMap for fast retrievals based on the method's
     * signature.
     */
    private final HashMap<String, InitializerMethod> initializers;
    /** Fully qualified name of the class. */
    private final String clazz;
    /** Path to file where the class exists. */
    private final Path path;

    /**
     * Creates an instance.
     *
     * @param clazz Fully qualified name.
     * @param path Path to the file where the class exists.
     */
    private Class(String clazz, Path path) {
      this.initializers = new HashMap<>();
      this.clazz = clazz;
      this.path = path;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Class)) {
        return false;
      }
      Class other = (Class) o;
      return clazz.equals(other.clazz) && path.equals(other.path);
    }

    @Override
    public int hashCode() {
      return Objects.hash(clazz, path);
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
      InitializerMethod observedInitializer =
          this.initializers.get(fieldInitializationNode.getInitializerMethod());
      if (observedInitializer != null) {
        // Method has been observed before, add the field info.
        observedInitializer.fields.add(fieldInitializationNode.getFieldName());
      } else {
        // Method has not been observed before, create and add the field info.
        InitializerMethod method =
            new InitializerMethod(fieldInitializationNode.getInitializerMethod());
        method.fields.add(fieldInitializationNode.getFieldName());
        // Add to list of observed.
        this.initializers.put(method.signature, method);
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
      for (InitializerMethod m : this.initializers.values()) {
        if (m.fields.size() > maxScore) {
          maxScore = m.fields.size();
          maxMethod = m;
        }
      }
      return maxMethod == null ? null : new OnMethod(path, clazz, maxMethod.signature);
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
      if (this == o) {
        return true;
      }
      if (!(o instanceof InitializerMethod)) {
        return false;
      }
      InitializerMethod other = (InitializerMethod) o;
      return signature.equals(other.signature);
    }

    @Override
    public int hashCode() {
      return Objects.hash(signature);
    }
  }
}
