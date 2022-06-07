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
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.google.common.base.Preconditions;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
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

  private static void walk(Node cursor, List<Node> candidates, Predicate<Node> predicate) {
    cursor.walk(
        Node.TreeTraversal.DIRECT_CHILDREN,
        node -> {
          if (!isTopLevelDeclaration(node)) {
            walk(node, candidates, predicate);
          }
          if (predicate.test(node)) {
            candidates.add(node);
          }
        });
  }

  private static TypeDeclaration<?> findTopLevelClassDeclarationOnCompilationUnit(
      CompilationUnit tree, String name) {
    Optional<EnumDeclaration> enumDeclaration = tree.getEnumByName(name);
    if (enumDeclaration.isPresent()) {
      return enumDeclaration.get();
    }
    Optional<ClassOrInterfaceDeclaration> interfaceDeclaration = tree.getInterfaceByName(name);
    return interfaceDeclaration.orElseGet(() -> tree.getClassByName(name).orElse(null));
  }

  public static Node findTopLevelDirectInnerClass(Node cursor, String name) {
    List<Node> nodes = new ArrayList<>();
    cursor.walk(
        Node.TreeTraversal.DIRECT_CHILDREN,
        node -> {
          if (isDeclarationWithName(node, name)) {
            nodes.add(node);
          }
        });
    Preconditions.checkArgument(
        nodes.size() > 0, "Could not find inner class " + name + " on:\n" + cursor);
    return nodes.get(0);
  }

  private static Node findTopLevelInnerClass(Node cursor, String name, int index) {
    final List<Node> candidates = new ArrayList<>();
    walk(
        cursor,
        candidates,
        node -> {
          Optional<Node> parent = node.getParentNode();
          if (!parent.isPresent()) {
            return false;
          }
          if (parent.get().equals(cursor)) {
            return false;
          }
          return isDeclarationWithName(node, name);
        });
    return candidates.get(index);
  }

  private static boolean isTopLevelDeclaration(Node node) {
    return node instanceof ClassOrInterfaceDeclaration
        || node instanceof EnumDeclaration
        || (node instanceof ObjectCreationExpr
            && ((ObjectCreationExpr) node).getAnonymousClassBody().isPresent());
  }

  private static boolean isDeclarationWithName(Node node, String name) {
    if (node instanceof ClassOrInterfaceDeclaration) {
      if (((ClassOrInterfaceDeclaration) node).getNameAsString().equals(name)) {
        return true;
      }
    }
    if (node instanceof EnumDeclaration) {
      return ((EnumDeclaration) node).getNameAsString().equals(name);
    }
    return false;
  }

  private static Node findTopLevelAnonymousClass(Node cursor, int index) {
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
    return candidates.get(index);
  }

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
    if (node instanceof ObjectCreationExpr) {
      ObjectCreationExpr objExp = (ObjectCreationExpr) node;
      return objExp.getAnonymousClassBody().orElse(null);
    }
    return null;
  }

  public static NodeList<BodyDeclaration<?>> getClassOrInterfaceOrEnumDeclarationMembersByFlatName(
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
    List<String> keys = new ArrayList<>(Arrays.asList(flatNameExcludingPackageName.split("\\$")));
    Node cursor = findTopLevelClassDeclarationOnCompilationUnit(cu, keys.get(0));
    keys.remove(0);
    for (String key : keys) {
      String indexString = extractIntegerFromBeginningOfStringInString(key);
      String actualName = key.substring(indexString.length());
      int index = indexString.equals("") ? 0 : Integer.parseInt(indexString) - 1;
      Preconditions.checkNotNull(cursor);
      if (key.matches("\\d+")) {
        cursor = findTopLevelAnonymousClass(cursor, index);
      } else {
        cursor =
            indexString.equals("")
                ? findTopLevelDirectInnerClass(cursor, actualName)
                : findTopLevelInnerClass(cursor, actualName, index);
      }
    }
    return getMembersOfNode(cursor);
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
