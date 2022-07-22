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

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.google.common.base.Preconditions;
import edu.ucr.cs.riple.injector.exceptions.TargetClassNotFound;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;

/** A utility class. */
public class Helper {

  /**
   * Extracts the callable simple name from callable signature. (e.g. in input "run(Object i)"
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
      if (current == '(') break;
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
   * Checks if callableDec's signature is equals to signature passed in the argument.
   *
   * @param callableDec callable declaration node.
   * @param signature signature of the callable in string.
   * @return true, if signature matches the callable and false otherwise.
   */
  public static boolean matchesCallableSignature(
      CallableDeclaration<?> callableDec, String signature) {
    // match simple names.
    if (!callableDec.getName().toString().equals(extractCallableName(signature))) {
      return false;
    }
    // match parameter types.
    List<String> paramTypesFromCallableDeclaration =
        extractParamTypesOfCallableInString(callableDec);
    List<String> paramTypesFromCallableSignature = extractParamTypesOfCallableInString(signature);
    if (paramTypesFromCallableDeclaration.size() != paramTypesFromCallableSignature.size()) {
      return false;
    }
    int size = paramTypesFromCallableDeclaration.size();
    for (int i = 0; i < size; i++) {
      String callableType = paramTypesFromCallableDeclaration.get(i);
      String signatureType = paramTypesFromCallableSignature.get(i);
      if (signatureType.equals(callableType)) {
        continue;
      }
      String simpleCallableType = simpleName(callableType);
      String simpleSignatureType = simpleName(signatureType);
      if (simpleCallableType.equals(simpleSignatureType)) {
        continue;
      }
      return false;
    }
    return true;
  }

  /**
   * Extracts parameters type from a {@link CallableDeclaration}.
   *
   * @param callableDec callable declaration instance.
   * @return List of parameters type in string.
   */
  private static List<String> extractParamTypesOfCallableInString(
      CallableDeclaration<?> callableDec) {
    ArrayList<String> paramTypes = new ArrayList<>();
    for (Parameter param : callableDec.getParameters()) {
      if (param != null) {
        String typeInString = param.getType().asString();
        if (param.isVarArgs()) {
          typeInString += "...";
        }
        paramTypes.add(typeInString);
      }
    }
    return paramTypes;
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
   * Finds Top-Level type declaration within a {@link CompilationUnit} tree by name, this node is
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
   * Locates the inner class with the given name at specific index.
   *
   * @param cursor Starting node for traversal.
   * @param name name of the inner class.
   * @param index index of the desired node among the candidates.
   * @return inner class with the given name and index.
   * @throws TargetClassNotFound if the target class is not found.
   */
  private static Node findInnerClass(Node cursor, String name, int index)
      throws TargetClassNotFound {
    final List<Node> candidates = new ArrayList<>();
    walk(cursor, candidates, node -> isDeclarationWithName(node, name));
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
  private static boolean isTypeDeclarationOrAnonymousClass(Node node) {
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
   * Locates an anonymous class at specific index.
   *
   * @param cursor Starting node for traversal.
   * @param index index.
   * @return anonymous class at specific index.
   * @throws TargetClassNotFound if the target class is not found.
   */
  private static Node findAnonymousClass(Node cursor, int index) throws TargetClassNotFound {
    final List<Node> candidates = new ArrayList<>();
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
    return null;
  }

  /**
   * Returns {@link NodeList} containing all members of an Enum/Interface/Class/Annotation
   * Declaration by flat name from a compilation unit tree.
   *
   * @param cu Compilation Unit tree instance.
   * @param flatName Flat name in string.
   * @return {@link NodeList} containing all members
   * @throws TargetClassNotFound if the target class is not found.
   */
  public static NodeList<BodyDeclaration<?>> getClassOrInterfaceOrEnumDeclarationMembersByFlatName(
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
        cursor = findAnonymousClass(cursor, index);
      } else {
        cursor =
            indexString.equals("")
                ? findDirectInnerClass(cursor, actualName)
                : findInnerClass(cursor, actualName, index);
      }
    }
    return getMembersOfNode(cursor);
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
   * Extracts the package name from fully qualified name. (e.g. for "{@code a.c.b.Foo<a.b.Bar, a.c.b.Foo>}" will return "{@code a.c.b").
   * @param Fully qualified name in String.
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
   * Returns a list of parameters extracted from callable signature. (e.g. for "{@code foo(@  CustomAnnot  (exp, exp2) a.c.b.Foo<a.b.Bar, a.c.b.Foo>, java.lang.Object)}" will return "{@code [a.c.b.Foo<a.b.Bar, a.c.b.Foo>, java.lang.Object]").
   * @param signature callable signature.
   * @return List of extracted parameters types.
   */
  private static List<String> extractParamTypesOfCallableInString(String signature) {
    signature = signature.substring(signature.indexOf("("));
    signature = signature.substring(1, signature.length() - 1);
    int index = 0;
    int generic_level = 0;
    List<String> ans = new ArrayList<>();
    StringBuilder tmp = new StringBuilder();
    while (index < signature.length()) {
      char c = signature.charAt(index);
      switch (c) {
        case '@':
          while (signature.charAt(index + 1) == ' ' && index + 1 < signature.length()) {
            index++;
          }
          int annot_level = 0;
          boolean finished = false;
          while (!finished && index < signature.length()) {
            if (signature.charAt(index) == '(') {
              ++annot_level;
            }
            if (signature.charAt(index) == ')') {
              --annot_level;
            }
            if (signature.charAt(index) == ' ' && annot_level == 0) {
              finished = true;
            }
            index++;
          }
          index--;
          break;
        case '<':
          generic_level++;
          tmp.append(c);
          break;
        case '>':
          generic_level--;
          tmp.append(c);
          break;
        case ',':
          if (generic_level == 0) {
            ans.add(tmp.toString());
            tmp = new StringBuilder();
          } else {
            tmp.append(c);
          }
          break;
        default:
          tmp.append(c);
      }
      index++;
    }
    if (signature.length() > 0 && generic_level == 0) {
      ans.add(tmp.toString().strip());
    }
    return ans;
  }

  /**
   * Creates a progress bar.
   *
   * @param task Task name.
   * @param steps Number of required steps to complete task.
   * @return Progress bar instance.
   */
  public static ProgressBar createProgressBar(String task, int steps) {
    return new ProgressBar(
        task,
        steps,
        1000,
        System.out,
        ProgressBarStyle.ASCII,
        "",
        1,
        false,
        null,
        ChronoUnit.SECONDS,
        0L,
        Duration.ZERO);
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
}
