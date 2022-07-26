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

package edu.ucr.cs.riple.injector;

import com.github.javaparser.ast.body.CallableDeclaration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Utility class to match {@link CallableDeclaration} with their signatures in {@code String}. */
public class SignatureMatcher {

  /** Simple name of the callable. */
  private final String callableName;
  /** List of parameters detected from signature in string. */
  private final List<String> parameterTypes;

  /**
   * Constructor to make a matcher instance.
   *
   * @param signature Signature to process.
   */
  public SignatureMatcher(String signature) {
    this.callableName = Helper.extractCallableName(signature);
    this.parameterTypes = extractParameterTypesFromSignature(signature);
  }

  /**
   * Returns a list of parameters extracted from callable signature. (e.g. for "{@code foo(@  CustomAnnot  (exp, exp2) a.c.b.Foo<a.b.Bar, a.c.b.Foo>, java.lang.Object)}" will return "{@code [a.c.b.Foo<a.b.Bar, a.c.b.Foo>, java.lang.Object]").
   * @param signature callable signature.
   * @return List of extracted parameters types.
   */
  private static List<String> extractParameterTypesFromSignature(String signature) {
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
   * Extracts parameters type from a {@link CallableDeclaration}.
   *
   * @param callableDec callable declaration instance.
   * @return List of parameters type in string.
   */
  private static List<String> extractParameterTypesFromCallableDeclaration(
      CallableDeclaration<?> callableDec) {
    return callableDec.getParameters().stream()
        .map(
            parameter -> {
              String type = parameter.getType().asString();
              return parameter.isVarArgs() ? type + "..." : type;
            })
        .collect(Collectors.toList());
  }

  /**
   * Checks if callableDec's signature is equals to parsed signature.
   *
   * @param callableDec callable declaration node.
   * @return true, if signature matches the callable and false otherwise.
   */
  public boolean matchesCallableDeclaration(CallableDeclaration<?> callableDec) {
    // match callable names.
    if (!callableDec.getName().toString().equals(callableName)) {
      return false;
    }
    // match parameter types.
    List<String> paramTypesFromCallableSignature =
        extractParameterTypesFromCallableDeclaration(callableDec);
    if (parameterTypes.size() != paramTypesFromCallableSignature.size()) {
      return false;
    }
    int size = parameterTypes.size();
    for (int i = 0; i < size; i++) {
      String callableType = parameterTypes.get(i);
      String signatureType = paramTypesFromCallableSignature.get(i);
      if (signatureType.equals(callableType)) {
        continue;
      }
      String simpleCallableType = Helper.simpleName(callableType);
      String simpleSignatureType = Helper.simpleName(signatureType);
      if (simpleCallableType.equals(simpleSignatureType)) {
        continue;
      }
      // Param types are different.
      return false;
    }
    return true;
  }
}
