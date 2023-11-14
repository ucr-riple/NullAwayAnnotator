/*
 * Copyright (c) 2022 University of California, Riverside.
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

package edu.ucr.cs.riple.injector;

import com.github.javaparser.Range;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.WildcardType;
import com.google.common.base.Preconditions;
import edu.ucr.cs.riple.injector.exceptions.TargetClassNotFound;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** A utility class. */
public class Helper {

  /**
   * Extracts the callable simple name from callable signature. (e.g. on input "run(Object i)"
   * returns "run").
   *
   * @param signature callable signature in string.
   * @return callable simple name.
   */
  public static String extractCallableName(String signature) {
    StringBuilder ans = new StringBuilder();
    int level = 0;
    for (int i = 0; i < signature.length(); i++) {
      char current = signature.charAt(i);
      if (current == '(') {
        break;
      }
      switch (current) {
        case '>':
          ++level;
          break;
        case '<':
          --level;
          break;
        default:
          if (level == 0) {
            ans.append(current);
          }
      }
    }
    return ans.toString();
  }

  /**
   * Walks on the AST starting from the cursor in {@link
   * com.github.javaparser.ast.Node.TreeTraversal#DIRECT_CHILDREN} manner, and adds all visiting
   * nodes which holds the predicate.
   *
   * @param cursor starting node for traversal.
   * @param candidates list of candidates, an empty list should be passed at the call site, accepted
   *     visited nodes will be added to this list.
   * @param predicate predicate to check if a node should be added to list of candidates.
   */
  private static void walk(Node cursor, List<Node> candidates, Predicate<Node> predicate) {
    cursor.walk(
        Node.TreeTraversal.DIRECT_CHILDREN,
        node -> {
          if (!isTypeDeclarationOrAnonymousClass(node)) {
            walk(node, candidates, predicate);
          }
          if (predicate.test(node)) {
            candidates.add(node);
          }
        });
  }

  /**
   * Finds Top-Level type declaration within a {@link CompilationUnit} tree by name, this node is a
   * direct child of the compilation unit tree and can be: [{@link ClassOrInterfaceDeclaration},
   * {@link EnumDeclaration}, {@link AnnotationDeclaration}].
   *
   * @param tree instance of compilation unit tree.
   * @param name name of the declaration.
   * @return the typeDeclaration with the given name.
   * @throws TargetClassNotFound if the target class is not found.
   */
  @Nonnull
  private static TypeDeclaration<?> findTopLevelClassDeclarationOnCompilationUnit(
      CompilationUnit tree, String name) throws TargetClassNotFound {
    Optional<ClassOrInterfaceDeclaration> classDeclaration = tree.getClassByName(name);
    if (classDeclaration.isPresent()) {
      return classDeclaration.get();
    }
    Optional<EnumDeclaration> enumDeclaration = tree.getEnumByName(name);
    if (enumDeclaration.isPresent()) {
      return enumDeclaration.get();
    }
    Optional<AnnotationDeclaration> annotationDeclaration =
        tree.getAnnotationDeclarationByName(name);
    if (annotationDeclaration.isPresent()) {
      return annotationDeclaration.get();
    }
    Optional<ClassOrInterfaceDeclaration> interfaceDeclaration = tree.getInterfaceByName(name);
    if (interfaceDeclaration.isPresent()) {
      return interfaceDeclaration.get();
    }
    Optional<RecordDeclaration> recordDeclaration = tree.getRecordByName(name);
    if (recordDeclaration.isPresent()) {
      return recordDeclaration.get();
    }
    throw new TargetClassNotFound("Top-Level", name, tree);
  }

  /**
   * Locates the inner class with the given name which is directly connected to cursor.
   *
   * @param cursor Parent node of inner class.
   * @param name Name of the inner class.
   * @return inner class with the given name.
   * @throws TargetClassNotFound if the target class is not found.
   */
  private static Node findDirectInnerClass(Node cursor, String name) throws TargetClassNotFound {
    List<Node> nodes = new ArrayList<>();
    cursor.walk(
        Node.TreeTraversal.DIRECT_CHILDREN,
        node -> {
          if (isDeclarationWithName(node, name)) {
            nodes.add(node);
          }
        });
    if (nodes.size() == 0) {
      throw new TargetClassNotFound("Direct-Inner-Class", name, cursor);
    }
    return nodes.get(0);
  }

  /**
   * Locates the non-direct inner class with the given name at specific index.
   *
   * @param cursor Starting node for traversal.
   * @param name name of the inner class.
   * @param index index of the desired node among the candidates.
   * @return inner class with the given name and index.
   * @throws TargetClassNotFound if the target class is not found.
   */
  private static Node findNonDirectInnerClass(Node cursor, String name, int index)
      throws TargetClassNotFound {
    final List<Node> candidates = new ArrayList<>();
    walk(
        cursor,
        candidates,
        node ->
            isDeclarationWithName(node, name)
                && node.getParentNode().isPresent()
                && !node.getParentNode().get().equals(cursor));
    if (index >= candidates.size()) {
      throw new TargetClassNotFound("Non-Direct-Inner-Class", index + name, cursor);
    }
    return candidates.get(index);
  }

  /**
   * Checks if node is a type declaration or an anonymous class.
   *
   * @param node Node instance.
   * @return true if is a type declaration or an anonymous class and false otherwise.
   */
  public static boolean isTypeDeclarationOrAnonymousClass(Node node) {
    return node instanceof ClassOrInterfaceDeclaration
        || node instanceof EnumDeclaration
        || node instanceof AnnotationDeclaration
        || (node instanceof ObjectCreationExpr
            && ((ObjectCreationExpr) node).getAnonymousClassBody().isPresent());
  }

  /**
   * Checks if the node is of type {@link TypeDeclaration} and a specific name.
   *
   * @param node input node.
   * @param name name.
   * @return true the node is subtype of TypeDeclaration and has the given name.
   */
  private static boolean isDeclarationWithName(Node node, String name) {
    if (node instanceof TypeDeclaration<?>) {
      return ((TypeDeclaration<?>) node).getNameAsString().equals(name);
    }
    return false;
  }

  /**
   * Locates an anonymous class or enum constant at specific index.
   *
   * @param cursor Starting node for traversal.
   * @param index index.
   * @return anonymous class or enum constant at specific index.
   * @throws TargetClassNotFound if the target class is not found.
   */
  private static Node findAnonymousClassOrEnumConstant(Node cursor, int index)
      throws TargetClassNotFound {
    final List<Node> candidates = new ArrayList<>();
    if (cursor instanceof EnumDeclaration) {
      // According to the Java language specification, enum constants are the first members of
      // enums. In JavaParser's data structures, enum constants are stored after all other members
      // of the enum which does not conform to how javac assigns flat names. We prioritize visiting
      // enum constants first.
      NodeList<EnumConstantDeclaration> constants = ((EnumDeclaration) cursor).getEntries();
      if (index < constants.size()) {
        return constants.get(index);
      }
      index -= constants.size();
    }
    walk(
        cursor,
        candidates,
        node -> {
          if (node instanceof ObjectCreationExpr) {
            return ((ObjectCreationExpr) node).getAnonymousClassBody().isPresent();
          }
          return false;
        });
    if (index >= candidates.size()) {
      throw new TargetClassNotFound("Top-Level-Anonymous-Class", "$" + index, cursor);
    }
    return candidates.get(index);
  }

  /**
   * Returns members of the type declaration.
   *
   * @param node Node instance.
   * @return {@link NodeList} containing member of node.
   */
  private static NodeList<BodyDeclaration<?>> getMembersOfNode(Node node) {
    if (node == null) {
      return null;
    }
    if (node instanceof EnumDeclaration) {
      return ((EnumDeclaration) node).getMembers();
    }
    if (node instanceof ClassOrInterfaceDeclaration) {
      return ((ClassOrInterfaceDeclaration) node).getMembers();
    }
    if (node instanceof AnnotationDeclaration) {
      return ((AnnotationDeclaration) node).getMembers();
    }
    if (node instanceof ObjectCreationExpr) {
      return ((ObjectCreationExpr) node).getAnonymousClassBody().orElse(null);
    }
    if (node instanceof EnumConstantDeclaration) {
      return ((EnumConstantDeclaration) node).getClassBody();
    }
    if (node instanceof RecordDeclaration) {
      return ((RecordDeclaration) node).getMembers();
    }
    return null;
  }

  /**
   * Returns {@link NodeList} containing all members of an
   * Enum/Interface/Class/AnonymousClass/Annotation Declaration by flat name from a compilation unit
   * tree.
   *
   * @param cu Compilation Unit tree instance.
   * @param flatName Flat name in string.
   * @return {@link NodeList} containing all members
   * @throws TargetClassNotFound if the target class is not found.
   */
  public static NodeList<BodyDeclaration<?>> getTypeDeclarationMembersByFlatName(
      CompilationUnit cu, String flatName) throws TargetClassNotFound {
    String packageName;
    Optional<PackageDeclaration> packageDeclaration = cu.getPackageDeclaration();
    if (packageDeclaration.isPresent()) {
      packageName = packageDeclaration.get().getNameAsString();
    } else {
      packageName = "";
    }
    Preconditions.checkArgument(
        flatName.startsWith(packageName),
        "Package name of compilation unit is incompatible with class name: "
            + packageName
            + " : "
            + flatName);
    String flatNameExcludingPackageName =
        packageName.equals("") ? flatName : flatName.substring(packageName.length() + 1);
    List<String> keys = new ArrayList<>(Arrays.asList(flatNameExcludingPackageName.split("\\$")));
    Node cursor = findTopLevelClassDeclarationOnCompilationUnit(cu, keys.get(0));
    keys.remove(0);
    for (String key : keys) {
      String indexString = extractIntegerFromBeginningOfStringInString(key);
      String actualName = key.substring(indexString.length());
      int index = indexString.equals("") ? 0 : Integer.parseInt(indexString) - 1;
      Preconditions.checkNotNull(cursor);
      if (key.matches("\\d+")) {
        cursor = findAnonymousClassOrEnumConstant(cursor, index);
      } else {
        cursor =
            indexString.equals("")
                ? findDirectInnerClass(cursor, actualName)
                : findNonDirectInnerClass(cursor, actualName, index);
      }
    }
    return getMembersOfNode(cursor);
  }

  /**
   * Locates a variable declaration expression in the tree of a {@link CallableDeclaration} with the
   * given name. Please note that the target local variable must be declared inside a callable
   * declaration and local variables inside initializer blocks or lambdas are not supported.
   *
   * @param encMethod The enclosing method which the variable is declared in.
   * @param varName The name of the variable.
   * @return The variable declaration expression, or null if it is not found.
   */
  @Nullable
  public static VariableDeclarationExpr locateVariableDeclarationExpr(
      CallableDeclaration<?> encMethod, String varName) {
    // Should not visit inner nodes of inner methods in the given method, since the given
    // method should be the closest enclosing method of the target local variable. Therefore, we
    // use DirectMethodParentIterator to skip inner methods.
    Iterator<Node> treeIterator = new DirectMethodParentIterator(encMethod);
    while (treeIterator.hasNext()) {
      Node n = treeIterator.next();
      if (n instanceof VariableDeclarationExpr) {
        VariableDeclarationExpr v = (VariableDeclarationExpr) n;
        if (v.getVariables().stream()
            .anyMatch(variableDeclarator -> variableDeclarator.getNameAsString().equals(varName))) {
          return v;
        }
      }
    }
    return null;
  }

  /**
   * Extracts the type of the given node implementing {@link NodeWithAnnotations}.
   *
   * @param node the node.
   * @return the type of the node.
   */
  public static Type getTypeFromNode(NodeWithAnnotations<?> node) {
    if (node instanceof MethodDeclaration) {
      return ((MethodDeclaration) node).getType();
    }
    if (node instanceof FieldDeclaration) {
      return ((FieldDeclaration) node).getElementType();
    }
    if (node instanceof VariableDeclarationExpr) {
      NodeList<VariableDeclarator> decls = ((VariableDeclarationExpr) node).getVariables();
      for (VariableDeclarator v : decls) {
        // All declared variables in a VariableDeclarationExpr have the same type.
        // (e.g. Foo a, b, c;)
        return v.getType();
      }
    }
    if (node instanceof Parameter) {
      return ((Parameter) node).getType();
    }
    if (node instanceof VariableDeclarator) {
      return ((VariableDeclarator) node).getType();
    }
    if (node instanceof Type) {
      return ((Type) node);
    }
    throw new RuntimeException("Unknown node type: " + node.getClass() + " " + node);
  }

  /**
   * Extracts the type of the given node implementing {@link NodeWithAnnotations}.
   *
   * @param node the node.
   * @return the type of the node.
   */
  public static Type getType(NodeWithAnnotations<?> node) {
    // Currently, we only annotate the element types (contents) of an array, not the pointer
    // itself.
    // TODO: This should be updated in a follow-up PR. This will reflect both type-use and
    // TODO: type-declaration annotations.
    Type type = getTypeFromNode(node);
    return type instanceof ArrayType ? ((ArrayType) type).getComponentType() : type;
  }

  /**
   * Helper method to check if a type is annotated with a specific annotation.
   *
   * @param type the type to check its annotations.
   * @param expr the annotation to check.
   * @return true if the node is annotated with the annotation.
   */
  public static boolean isAnnotatedWith(Type type, AnnotationExpr expr) {
    return type.getAnnotations().stream().anyMatch(annot -> annot.equals(expr));
  }

  /**
   * Helper method to check if a node is annotated with a specific annotation.
   *
   * @param node the node to check its annotations.
   * @param expr the annotation to check.
   * @return true if the node is annotated with the annotation.
   */
  public static boolean isAnnotatedWith(NodeWithAnnotations<?> node, AnnotationExpr expr) {
    return node.getAnnotations().stream().anyMatch(annot -> annot.equals(expr));
  }

  /**
   * Extracts the integer at the start of string (e.g. 129uid -> 129).
   *
   * @param key string containing the integer.
   * @return the integer at the start of the key, empty if no digit found at the beginning (e.g.
   *     u129 -> empty)
   */
  private static String extractIntegerFromBeginningOfStringInString(String key) {
    int index = 0;
    while (index < key.length()) {
      char c = key.charAt(index);
      if (!Character.isDigit(c)) {
        break;
      }
      index++;
    }
    return key.substring(0, index);
  }

  /**
   * Extracts simple name of fully qualified name. (e.g. for "{@code a.c.b.Foo<a.b.Bar, a.c.b.Foo>}"
   * will return "{@code Foo<Bar,Foo>}").
   *
   * @param name Fully qualified name.
   * @return simple name.
   */
  public static String simpleName(String name) {
    int index = 0;
    StringBuilder ans = new StringBuilder();
    StringBuilder tmp = new StringBuilder();
    while (index < name.length()) {
      char c = name.charAt(index);
      switch (c) {
        case ' ':
        case '<':
        case '>':
        case ',':
          ans.append(tmp);
          ans.append(c);
          tmp = new StringBuilder();
          break;
        case '.':
          tmp = new StringBuilder();
          break;
        default:
          tmp.append(c);
      }
      index++;
    }
    if (name.length() > 0) {
      ans.append(tmp);
    }
    return ans.toString().replaceAll(" ", "");
  }

  /**
   * Extracts the package name from fully qualified name. (e.g. for "{@code a.c.b.Foo<a.b.Bar,
   * a.c.b.Foo>}" will return "{@code a.c.b}").
   *
   * @param name Fully qualified name in String.
   * @return Package name.
   */
  public static String getPackageName(String name) {
    if (!name.contains(".")) {
      return null;
    }
    List<String> verified = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    int index = 0;
    while (index < name.length()) {
      char currentChar = name.charAt(index);
      if (currentChar == '.') {
        verified.add(current.toString());
        current = new StringBuilder();
      } else {
        if (Character.isAlphabetic(currentChar) || Character.isDigit(currentChar)) {
          current.append(currentChar);
        } else {
          break;
        }
      }
      index++;
    }
    return String.join(".", verified);
  }

  /**
   * Deserializes a Path instance from a string.
   *
   * @param serializedPath Serialized path to file.
   * @return The modified Path.
   */
  public static Path deserializePath(String serializedPath) {
    final String jarPrefix = "jar:";
    final String filePrefix = "file://";
    String path = serializedPath;
    if (serializedPath.startsWith(jarPrefix)) {
      path = path.substring(jarPrefix.length());
    }
    if (serializedPath.startsWith(filePrefix)) {
      path = path.substring(filePrefix.length());
    }
    // Keep only one occurrence of "/" from the beginning if more than one exists.
    path = Paths.get(path).toString();
    int start = 0;
    while (start + 1 < path.length()
        && path.charAt(start) == '/'
        && path.charAt(start + 1) == '/') {
      start++;
    }
    return Paths.get(path.substring(start));
  }

  /**
   * Used to check if src package declaration is under a specific root.
   *
   * @param path Path to src file.
   * @param rootPackage Root package simple name.
   * @return true if src has a package declaration and starts with root.
   */
  public static boolean srcIsUnderClassClassPath(Path path, String rootPackage) {
    try {
      CompilationUnit cu = StaticJavaParser.parse(path.toFile());
      Optional<PackageDeclaration> packageDeclaration = cu.getPackageDeclaration();
      return packageDeclaration
          .map(declaration -> declaration.getNameAsString().startsWith(rootPackage))
          .orElse(false);
    } catch (FileNotFoundException e) {
      throw new IllegalArgumentException("File not found: " + path, e);
    }
  }

  /**
   * Returns containing static initializer blocks of a {@link BodyDeclaration}.
   *
   * @param bodyDeclaration the body declaration to get its static initializer blocks.
   * @return the static initializer blocks of the body declaration.
   */
  public static Set<InitializerDeclaration> getStaticInitializerBlocks(
      BodyDeclaration<?> bodyDeclaration) {
    return bodyDeclaration.getChildNodes().stream()
        .filter(
            node ->
                node instanceof InitializerDeclaration
                    && ((InitializerDeclaration) node).isStatic())
        .map(node -> (InitializerDeclaration) node)
        .collect(Collectors.toSet());
  }

  /**
   * Finds the range of the simple name in the fully qualified name of the given type in the source
   * code. This is used to insert the type use annotations before the type simple name.
   *
   * @param type the type to find its range
   * @return the range of the type or null if the type does not have a range.
   */
  public static Range findSimpleNameRangeInTypeName(Type type) {
    if (type instanceof ClassOrInterfaceType) {
      ClassOrInterfaceType classOrInterfaceType = (ClassOrInterfaceType) type;
      if (classOrInterfaceType.getName().getRange().isEmpty()) {
        return null;
      }
      return ((ClassOrInterfaceType) type).getName().getRange().get();
    }
    if (type instanceof ArrayType) {
      return findSimpleNameRangeInTypeName(((ArrayType) type).getComponentType());
    }
    if (type instanceof PrimitiveType) {
      if (type.getRange().isEmpty()) {
        return null;
      }
      return type.getRange().get();
    }
    if (type instanceof WildcardType) {
      return findSimpleNameRangeInTypeName(((WildcardType) type).getExtendedType().orElse(null));
    }
    throw new RuntimeException(
        "Unexpected type to get range from: " + type + " : " + type.getClass());
  }

  /**
   * Iterates over children of a {@link CallableDeclaration} in a depth-first manner, skipping over
   * any {@link BodyDeclaration}.
   */
  public static final class DirectMethodParentIterator implements Iterator<Node> {

    private final ArrayDeque<Node> deque = new ArrayDeque<>();

    public DirectMethodParentIterator(CallableDeclaration<?> node) {
      deque.add(node);
    }

    @Override
    public boolean hasNext() {
      return !deque.isEmpty();
    }

    @Override
    public Node next() {
      Node next = deque.removeFirst();
      List<Node> children = next.getChildNodes();
      for (int i = children.size() - 1; i >= 0; i--) {
        Node child = children.get(i);
        if (!(child instanceof BodyDeclaration<?>)) {
          // Skip over any CallableDeclaration.
          deque.add(children.get(i));
        }
      }
      return next;
    }
  }
}
