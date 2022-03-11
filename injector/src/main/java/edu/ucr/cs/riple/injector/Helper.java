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
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

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
    if (!callableDec.getName().toString().equals(extractCallableName(signature))) return false;
    List<String> paramsTypesInSignature = extractParamTypesOfCallableInString(signature);
    List<String> paramTypes = extractParamTypesOfCallableInString(callableDec);
    if (paramTypes.size() != paramsTypesInSignature.size()) return false;
    for (String i : paramsTypesInSignature) {
      String found = null;
      String last_i = simpleName(i);
      for (String j : paramTypes) {
        String last_j = simpleName(j);
        if (j.equals(i) || last_i.equals(last_j)) found = j;
      }
      if (found == null) return false;
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
      CompilationUnit cu, String pkg, String name) {
    String classSimpleName = simpleName(name);
    if (pkg.equals(getPackageName(name))) {
      Optional<ClassOrInterfaceDeclaration> optional = cu.getClassByName(classSimpleName);
      if (!optional.isPresent()) {
        optional = cu.getInterfaceByName(classSimpleName);
        if (optional.isPresent()) {
          return optional.get();
        }
        Optional<EnumDeclaration> optionalEnumDeclaration = cu.getEnumByName(classSimpleName);
        if (optionalEnumDeclaration.isPresent()) {
          return optionalEnumDeclaration.get();
        }
      }
    }
    try {
      List<ClassOrInterfaceDeclaration> options =
          cu.getLocalDeclarationFromClassname(classSimpleName);
      for (ClassOrInterfaceDeclaration candidate : options) {
        if (candidate.getName().toString().equals(classSimpleName)) {
          return candidate;
        }
      }
    } catch (NoSuchElementException ignored) {
    }
    List<ClassOrInterfaceDeclaration> candidates =
        cu.findAll(
            ClassOrInterfaceDeclaration.class,
            classOrInterfaceDeclaration ->
                classOrInterfaceDeclaration.getName().toString().equals(classSimpleName));
    if (candidates.size() > 0) {
      return candidates.get(0);
    }
    List<EnumDeclaration> enumCandidates =
        cu.findAll(
            EnumDeclaration.class,
            classOrInterfaceDeclaration ->
                classOrInterfaceDeclaration.getName().toString().equals(classSimpleName));
    if (enumCandidates.size() > 0) {
      return enumCandidates.get(0);
    }
    return null;
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
}
