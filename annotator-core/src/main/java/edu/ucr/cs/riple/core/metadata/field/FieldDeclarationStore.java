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

package edu.ucr.cs.riple.core.metadata.field;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.ModuleInfo;
import edu.ucr.cs.riple.core.metadata.MetaData;
import edu.ucr.cs.riple.injector.Helper;
import edu.ucr.cs.riple.injector.exceptions.TargetClassNotFound;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.scanner.AnnotatorScanner;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * Stores field declaration data on classes. It can Detect multiple inline field declarations. An
 * annotation will be injected on top of the field declaration statement, and in inline multiple
 * field declarations that annotation will be considered for all declaring fields. This class is
 * used to detect these cases and adjust the suggested fix instances. (e.g. If we have Object f, i,
 * j; and a Fix suggesting f to be {@code Nullable}, this class will replace that fix with a fix
 * suggesting f, i, and j be {@code Nullable}.)
 */
public class FieldDeclarationStore extends MetaData<FieldDeclarationInfo> {

  /**
   * Output file name. It contains information about all classes existing in source code. Each line
   * has the format: (class-flat-name \t path to file containing the class). It is generated by
   * {@link AnnotatorScanner} checker.
   */
  public static final String FILE_NAME = "class_info.tsv";

  /**
   * Constructor for {@link FieldDeclarationStore}.
   *
   * @param config Annotator config.
   * @param module Information of the target module.
   */
  public FieldDeclarationStore(Config config, ModuleInfo module) {
    super(config, module.dir.resolve(FILE_NAME));
  }

  /**
   * Constructor for {@link FieldDeclarationStore}. Contents are accumulated from multiple sources.
   *
   * @param config Annotator config.
   * @param modules Information of set of modules.
   */
  public FieldDeclarationStore(Config config, ImmutableSet<ModuleInfo> modules) {
    super(
        config,
        modules.stream()
            .map(info -> info.dir.resolve(FILE_NAME))
            .collect(ImmutableSet.toImmutableSet()));
  }

  @Override
  protected FieldDeclarationInfo addNodeByLine(String[] values) {
    // Class flat name.
    String clazz = values[0];
    // Path to class.
    Path path = Helper.deserializePath(values[1]);
    CompilationUnit tree;
    try {
      tree = StaticJavaParser.parse(path);
      NodeList<BodyDeclaration<?>> members;
      try {
        members = Helper.getTypeDeclarationMembersByFlatName(tree, clazz);
      } catch (TargetClassNotFound notFound) {
        System.err.println(notFound.getMessage());
        return null;
      }
      FieldDeclarationInfo info = new FieldDeclarationInfo(path, clazz);
      members.forEach(
          bodyDeclaration ->
              bodyDeclaration.ifFieldDeclaration(
                  fieldDeclaration -> {
                    NodeList<VariableDeclarator> vars = fieldDeclaration.getVariables();
                    info.addNewSetOfFieldDeclarations(
                        vars.stream()
                            .map(NodeWithSimpleName::getNameAsString)
                            .collect(ImmutableSet.toImmutableSet()));
                  }));
      return info.isEmpty() ? null : info;
    } catch (FileNotFoundException e) {
      return null;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
    FieldDeclarationInfo candidate =
        findNodeWithHashHint(node -> node.clazz.equals(clazz), FieldDeclarationInfo.hash(clazz));
    if (candidate == null) {
      // No inline multiple field declarations.
      return ImmutableSet.copyOf(fields);
    }
    Optional<ImmutableSet<String>> inLineGroupFieldDeclaration =
        candidate.fields.stream().filter(group -> !Collections.disjoint(group, fields)).findFirst();
    return inLineGroupFieldDeclaration.orElse(ImmutableSet.copyOf(fields));
  }

  /**
   * Creates a {@link OnField} instance targeting the passed field and class.
   *
   * @param clazz Enclosing class of the field.
   * @param field Field name.
   * @return {@link OnField} instance targeting the passed field and class.
   */
  public OnField getLocationOnField(String clazz, String field) {
    FieldDeclarationInfo candidate =
        findNodeWithHashHint(node -> node.clazz.equals(clazz), FieldDeclarationInfo.hash(clazz));
    Set<String> fieldNames = Sets.newLinkedHashSet();
    fieldNames.add(field);
    if (candidate == null) {
      // field is on byte code.
      return null;
    }
    return new OnField(candidate.pathToSourceFile, candidate.clazz, fieldNames);
  }
}
