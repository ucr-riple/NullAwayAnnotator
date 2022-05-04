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

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.Statement;
import com.google.common.base.Preconditions;
import edu.ucr.cs.riple.injector.ast.AnonymousClass;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;

public class Helper {

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
          if (level == 0) ans.append(current);
      }
    }
    return ans.toString();
  }

  public static boolean matchesCallableSignature(
      CallableDeclaration<?> callableDec, String signature) {
    if (!callableDec.getName().toString().equals(extractCallableName(signature))) {
      return false;
    }
    List<String> paramsTypesInSignature = extractParamTypesOfCallableInString(signature);
    List<String> paramTypes = extractParamTypesOfCallableInString(callableDec);
    if (paramTypes.size() != paramsTypesInSignature.size()) {
      return false;
    }
    for (String i : paramsTypesInSignature) {
      String found = null;
      String last_i = simpleName(i);
      for (String j : paramTypes) {
        String last_j = simpleName(j);
        if (j.equals(i) || last_i.equals(last_j)) found = j;
      }
      if (found == null) {
        return false;
      }
      paramTypes.remove(found);
    }
    return true;
  }

  public static List<String> extractParamTypesOfCallableInString(
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

  public static TypeDeclaration<?> getClassOrInterfaceOrEnumDeclaration(
      CompilationUnit cu, String flatName) {
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
    List<String> keys = findKeysInClassFlatName(flatNameExcludingPackageName);
    TypeDeclaration<?> cursor = findTopLevelTypeDeclarationOnNode(cu, keys.get(0), 0);
    keys.remove(0);
    for (String key : keys) {
      key = key.startsWith("$") ? key.substring(1) : key;
      String indexString = extractIntegerFromBeginningOfStringInString(key);
      String actualName = key.substring(indexString.length());
      int index = indexString.equals("") ? 0 : Integer.parseInt(indexString) - 1;
      Preconditions.checkNotNull(cursor);
      if (key.matches("\\d+")) {
        cursor = findAnonymousClassOnTypeDeclaration(cursor, index);
      } else {
        cursor = findTopLevelTypeDeclarationOnNode(cursor, actualName, index);
      }
    }
    return cursor;
  }

  /**
   * Extract the integer at the start of string (e.g. 129uid -> 129).
   *
   * @param key string containing the integer.
   * @return the integer at the start of the key, empty if no digit found at the beginning (e.g.
   *     u129 -> empty)
   */
  public static String extractIntegerFromBeginningOfStringInString(String key) {
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
   * Finds the top level class declaration on Node including local class declarations in method
   * bodies.
   *
   * @param node Node to perform the search on
   * @param actualName Actual name of the class excluding the index (e.g. $1Helper -> Helper)
   * @param index Index of the candidates (e.g. $1Helper -> 0)
   * @return The target class
   */
  private static TypeDeclaration<?> findTopLevelTypeDeclarationOnNode(
      Node node, String actualName, int index) {
    List<TypeDeclaration<?>> candidates =
        node.getChildNodes()
            .stream()
            .flatMap(
                child -> {
                  if (child instanceof MethodDeclaration) {
                    MethodDeclaration method = ((MethodDeclaration) child);
                    if (method.getBody().isPresent()) {
                      return method
                          .getBody()
                          .get()
                          .getStatements()
                          .stream()
                          .filter(Statement::isLocalClassDeclarationStmt)
                          .map(
                              statement ->
                                  statement.asLocalClassDeclarationStmt().getClassDeclaration());
                    }
                  }
                  return Stream.of(child);
                })
            .filter(
                candidate ->
                    TypeDeclaration.class.isAssignableFrom(candidate.getClass())
                        && ((TypeDeclaration<?>) candidate).getNameAsString().equals(actualName))
            .map((Function<Node, TypeDeclaration<?>>) child -> (TypeDeclaration<?>) child)
            .collect(Collectors.toList());
    Preconditions.checkArgument(
        index < candidates.size(),
        "Could not find class at index: "
            + index
            + " with name "
            + actualName
            + " On node:\n"
            + node);
    return candidates.get(index);
  }

  /**
   * Finds the Anonymous class defined on a type declaration.
   *
   * @param cursor type declaration containing the Anonymous class.
   * @param index index of the target Anonymous class on cursor.
   * @return the Anonymous class tree.
   */
  private static TypeDeclaration<?> findAnonymousClassOnTypeDeclaration(Node cursor, int index) {
    List<AnonymousClass> anonymousClasses = new ArrayList<>();
    final int[] i = {1};
    cursor.walk(
        Node.TreeTraversal.BREADTHFIRST,
        node -> {
          if (node instanceof ObjectCreationExpr && isInScopeOf(cursor, node)) {
            Optional<NodeList<BodyDeclaration<?>>> anonymousBody =
                ((ObjectCreationExpr) node).getAnonymousClassBody();
            anonymousBody.ifPresent(
                declarations ->
                    anonymousClasses.add(new AnonymousClass(String.valueOf(i[0]++), declarations)));
          }
        });
    Preconditions.checkArgument(
        index < anonymousClasses.size(), "Did not found the anonymous class at index: " + index);
    return anonymousClasses.get(index);
  }

  public static boolean isInScopeOf(Node parent, Node child) {
    Node current = child;
    while (current != null && !current.equals(parent)) {
      current = current.getParentNode().orElse(null);
      if (current instanceof ObjectCreationExpr
          || current instanceof ClassOrInterfaceDeclaration
          || current instanceof EnumDeclaration) {
        return current.equals(parent);
      }
    }
    return current != null;
  }

  /**
   * Gets the keys comprising a flat name (e.g. A.B$1C.D$1 will be a list of [A, B, $1C, D, $1]
   *
   * @param flatName Flat name compiled by javac
   * @return List of keys in flat name
   */
  public static List<String> findKeysInClassFlatName(String flatName) {
    int index = 0;
    List<String> ans = new ArrayList<>();
    StringBuilder temp = new StringBuilder();
    while (index < flatName.length()) {
      char current = flatName.charAt(index);
      if (current == '.') {
        ans.add(temp.toString());
        temp = new StringBuilder();
        index++;
        continue;
      }
      if (current == '$') {
        ans.add(temp.toString());
        temp = new StringBuilder("$");
        index++;
        continue;
      }
      temp.append(current);
      index++;
    }
    ans.add(temp.toString());
    return ans;
  }

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
    if (name.length() > 0) ans.append(tmp);
    return ans.toString().replaceAll(" ", "");
  }

  public static String getPackageName(String name) {
    if (!name.contains(".")) {
      return null;
    }
    int index = name.lastIndexOf(".");
    return name.substring(0, index);
  }

  public static List<String> extractParamTypesOfCallableInString(String signature) {
    signature = signature.substring(signature.indexOf("(")).replace("(", "").replace(")", "");
    int index = 0;
    int generic_level = 0;
    List<String> ans = new ArrayList<>();
    StringBuilder tmp = new StringBuilder();
    while (index < signature.length()) {
      char c = signature.charAt(index);
      switch (c) {
        case '@':
          while (signature.charAt(index + 1) == ' ' && index + 1 < signature.length()) index++;
          int annot_level = 0;
          boolean finished = false;
          while (!finished && index < signature.length()) {
            if (signature.charAt(index) == '(') ++annot_level;
            if (signature.charAt(index) == ')') --annot_level;
            if (signature.charAt(index) == ' ' && annot_level == 0) finished = true;
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
          } else tmp.append(c);
          break;
        default:
          tmp.append(c);
      }
      index++;
    }
    if (signature.length() > 0 && generic_level == 0) ans.add(tmp.toString());
    return ans;
  }

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
}
