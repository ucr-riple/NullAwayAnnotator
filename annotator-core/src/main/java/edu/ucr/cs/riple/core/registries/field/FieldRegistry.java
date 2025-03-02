/*
 * MIT License
 *
 * Copyright (c) 2022 Nima Karimipour
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package edu.ucr.cs.riple.core.registries.field;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.utils.Pair;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Sets;
import edu.ucr.cs.riple.core.Context;
import edu.ucr.cs.riple.core.module.ModuleConfiguration;
import edu.ucr.cs.riple.core.registries.Registry;
import edu.ucr.cs.riple.injector.Injector;
import edu.ucr.cs.riple.injector.Printer;
import edu.ucr.cs.riple.injector.exceptions.TargetClassNotFound;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.OnClass;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.injector.util.ASTUtils;
import edu.ucr.cs.riple.scanner.Serializer;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Stores field declaration data on classes. It can Detect multiple inline field declarations. An
 * annotation will be injected on top of the field declaration statement, and in inline multiple
 * field declarations that annotation will be considered for all declaring fields. This class is
 * used to detect these cases and adjust the suggested fix instances. (e.g. If we have Object f, i,
 * j; and a Fix suggesting f to be {@code Nullable}, this class will replace that fix with a fix
 * suggesting f, i, and j be {@code Nullable}.)
 */
public class FieldRegistry extends Registry<ClassFieldRecord> {

  /**
   * A map from class flat name to a set of field names that are declared in that class but not
   * initialized at declaration.
   */
  private Multimap<String, String> uninitializedFields;

  /**
   * Constructor for {@link FieldRegistry}.
   *
   * @param module Information of the target module.
   */
  public FieldRegistry(ModuleConfiguration module, Context context) {
    this(ImmutableSet.of(module), context);
  }

  /**
   * Constructor for {@link FieldRegistry}. Contents are accumulated from multiple sources.
   *
   * @param modules Information of set of modules.
   */
  public FieldRegistry(ImmutableSet<ModuleConfiguration> modules, Context context) {
    super(
        modules.stream()
            .map(info -> info.dir.resolve(Serializer.CLASS_RECORD_FILE_NAME))
            .collect(ImmutableSet.toImmutableSet()),
        context);
  }

  @Override
  protected void setup() {
    super.setup();
    this.uninitializedFields = MultimapBuilder.hashKeys().hashSetValues().build();
  }

  @Override
  protected Builder<ClassFieldRecord> getBuilder() {
    return new Builder<>() {
      Pair<Path, CompilationUnit> lastParsedSourceFile = new Pair<>(null, null);

      @Override
      public ClassFieldRecord build(String[] values) {
        // This method is called with values in format of: [class flat name, path to source file].
        // To avoid parsing a source file multiple times, we keep the last parsed
        // source file in a reference.
        // This optimization is according to the assumption that Scanner
        // visits all classes within a single compilation unit tree consecutively.
        // Path to class.
        Path path = Printer.deserializePath(values[1]);
        CompilationUnit tree;
        if (lastParsedSourceFile.a != null && lastParsedSourceFile.a.equals(path)) {
          // Already visited.
          tree = lastParsedSourceFile.b;
        } else {
          // Not visited yet, parse the source file.
          tree = Injector.parse(path, context.config.languageLevel);
          lastParsedSourceFile = new Pair<>(path, tree);
        }
        if (tree == null) {
          return null;
        }
        NodeList<BodyDeclaration<?>> members;
        // Class flat name.
        String clazz = values[0];
        try {
          members = ASTUtils.getTypeDeclarationMembersByFlatName(tree, clazz);
        } catch (TargetClassNotFound notFound) {
          System.err.println(notFound.getMessage());
          return null;
        }
        ClassFieldRecord record = new ClassFieldRecord(path, clazz);
        members.forEach(
            bodyDeclaration ->
                bodyDeclaration.ifFieldDeclaration(
                    fieldDeclaration -> {
                      NodeList<VariableDeclarator> vars = fieldDeclaration.getVariables();
                      if (vars.getFirst().isEmpty()) {
                        // unexpected but just in case.
                        return;
                      }
                      record.addFieldDeclaration(fieldDeclaration);
                      // Collect uninitialized fields at declaration.
                      vars.forEach(
                          variableDeclarator -> {
                            String fieldName = variableDeclarator.getNameAsString();
                            if (variableDeclarator.getInitializer().isEmpty()) {
                              uninitializedFields.put(clazz, fieldName);
                            }
                          });
                    }));
        // We still want to keep the information about the class even if it has no field
        // declarations, so we can retrieve tha path to the file from the given class flat name.
        // This information is used in adding suppression annotations on class level.
        return record;
      }
    };
  }

  /**
   * Returns all field names declared within the same declaration statement for any field given in
   * the parameter.
   *
   * @param clazz Flat name of the enclosing class.
   * @param fields Subset of all fields declared within the same statement in the given class.
   * @return Set of all fields declared within that statement.
   */
  public ImmutableSet<String> getInLineMultipleFieldDeclarationsOnField(
      String clazz, Set<String> fields) {
    ClassFieldRecord candidate =
        findRecordWithHashHint(node -> node.clazz.equals(clazz), ClassFieldRecord.hash(clazz));
    if (candidate == null) {
      // No inline multiple field declarations.
      return ImmutableSet.copyOf(fields);
    }
    Optional<ClassFieldRecord.FieldDeclarationRecord> inLineGroupFieldDeclaration =
        candidate.fields.stream()
            .filter(group -> !Collections.disjoint(group.names, fields))
            .findFirst();
    if (inLineGroupFieldDeclaration.isPresent()) {
      return inLineGroupFieldDeclaration.get().names;
    }
    return ImmutableSet.copyOf(fields);
  }

  /**
   * Returns true if the given field is declared in the given class but not initialized at
   * declaration. A field declaration can contain multiple inline field declarations. This method
   * will return true if any of the declared fields is not initialized.
   *
   * @param field Location of field to check.
   * @return True if at least on of the given declarations at the given location is not initialized.
   */
  public boolean isUninitializedField(OnField field) {
    // According to javadoc, Multimap.get() returns an empty collection if key is not found,
    // therefore we do not need to check for key existence.
    return !Collections.disjoint(uninitializedFields.get(field.clazz), field.variables);
  }

  /**
   * Creates a {@link OnField} instance targeting the passed field and class.
   *
   * @param clazz Enclosing class of the field.
   * @param field Field name.
   * @return {@link OnField} instance targeting the passed field and class.
   */
  public OnField getLocationOnField(String clazz, String field) {
    ClassFieldRecord candidate =
        findRecordWithHashHint(node -> node.clazz.equals(clazz), ClassFieldRecord.hash(clazz));
    Set<String> fieldNames = Sets.newHashSet(field);
    if (candidate == null) {
      // field is on byte code.
      return null;
    }
    return new OnField(candidate.pathToSourceFile, candidate.clazz, fieldNames);
  }

  /**
   * Creates a {@link OnClass} instance targeting the passed classes flat name.
   *
   * @param clazz Enclosing class of the field.
   * @return {@link OnClass} instance targeting the passed classes flat name.
   */
  public OnClass getLocationOnClass(String clazz) {
    ClassFieldRecord candidate =
        findRecordWithHashHint(node -> node.clazz.equals(clazz), ClassFieldRecord.hash(clazz));
    if (candidate == null) {
      // class not observed in source code.
      return null;
    }
    return new OnClass(candidate.pathToSourceFile, candidate.clazz);
  }

  /**
   * Returns fields with public visibility and a non-primitive return type.
   *
   * @return ImmutableSet of fields location.
   */
  public ImmutableSet<OnField> getPublicFieldWithNonPrimitiveType() {
    ImmutableSet.Builder<OnField> builder = ImmutableSet.builder();
    contents
        .values()
        .forEach(
            record ->
                record.fields.forEach(
                    fieldDeclarationRecord -> {
                      if (fieldDeclarationRecord.isPublicFieldWithNonPrimitiveType()) {
                        builder.add(
                            new OnField(
                                record.pathToSourceFile,
                                record.clazz,
                                fieldDeclarationRecord.names));
                      }
                    }));
    return builder.build();
  }

  /**
   * Returns all declared fields in the given class.
   *
   * @return ImmutableSet of fields location.
   */
  public ImmutableSet<OnField> getDeclaredFieldsInClass(String clazz) {
    ImmutableSet.Builder<OnField> builder = ImmutableSet.builder();
    contents
        .values()
        .forEach(
            record -> {
              if (!record.clazz.equals(clazz)) {
                return;
              }
              record.fields.forEach(
                  fieldDeclarationRecord -> {
                    builder.add(
                        new OnField(
                            record.pathToSourceFile, record.clazz, fieldDeclarationRecord.names));
                  });
            });
    return builder.build();
  }

  /**
   * Checks if the given location is a field and declared in the module this registry is created
   * for.
   *
   * @param location Location of check.
   * @return True if the given location is a field and declared in the module this registry is
   *     created for.
   */
  public boolean declaredInModule(@Nullable Location location) {
    if (location == null || location.clazz.equals("null")) {
      return false;
    }
    if (!location.isOnField()) {
      return false;
    }
    OnField onField = location.toField();
    return findRecordWithHashHint(
            node ->
                node.clazz.equals(location.clazz)
                    && node.hasExactFieldDeclarationWithNames(onField.variables),
            ClassFieldRecord.hash(location.clazz))
        != null;
  }
}
