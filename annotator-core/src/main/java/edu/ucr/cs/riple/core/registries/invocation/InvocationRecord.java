/*
 * MIT License
 *
 * Copyright (c) 2025 Nima Karimipour
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

package edu.ucr.cs.riple.core.registries.invocation;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.registries.method.MethodRecord;
import edu.ucr.cs.riple.core.registries.region.Region;
import edu.ucr.cs.riple.core.util.ASTParser;
import edu.ucr.cs.riple.injector.util.SignatureMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class InvocationRecord {

  /**
   * Invocation record registry to find methods by name and class. and to find the called methods.
   */
  private final InvocationRecordRegistry registry;

  /**
   * A list of sets of method records. Each set contains the methods that are called at the same
   * depth.
   */
  private final List<Set<MethodRecord>> calls;

  public InvocationRecord(InvocationRecordRegistry registry) {
    this.calls = new ArrayList<>(3);
    this.registry = registry;
  }

  /**
   * Adds a set of method records to the record. The provided set contains methods invoked at the
   * current depth, which are called by methods from the previous depth.
   *
   * @param calls the set of method records to add.
   */
  public void pushCallers(Set<MethodRecord> calls) {
    this.calls.add(calls);
  }

  /**
   * Constructs a prompt for the call graph analysis. The prompt includes the source code of methods
   * at each depth level and consolidates multiple methods from the same class in the final output.
   *
   * @param parser the AST parser.
   * @return a prompt for the call graph.
   */
  public String constructCallGraphContext(ASTParser parser) {
    StringBuilder prompt = new StringBuilder();
    for (int i = 0; i < calls.size(); i++) {
      prompt.append("Depth: ").append(i).append("\n");
      Set<MethodRecord> methods = calls.get(i);
      Map<String, Set<MethodRecord>> classToMethods =
          methods.stream()
              .collect(Collectors.groupingBy(method -> method.location.clazz, Collectors.toSet()));
      for (String clazz : classToMethods.keySet()) {
        prompt.append(String.format("```java\nclass %s {\n", clazz));
        for (MethodRecord method : classToMethods.get(clazz)) {
          String methodBody =
              String.format(
                  "%s",
                  parser.getRegionSourceCode(
                          new Region(method.location.clazz, method.location.method))
                      .content);
          prompt.append(methodBody);
        }
        prompt.append("\n}\n```\n");
      }
    }
    return prompt.toString();
  }

  /**
   * Adds the requested methods to the record on the top of the stack. These methods are not present
   * in the invocation record and are only called within methods.
   *
   * @param requests Methods to add.
   */
  public void addRequestedMethods(ImmutableSet<String> requests) {
    Set<MethodRecord> existingMethods =
        calls.stream().flatMap(Set::stream).collect(Collectors.toSet());
    Set<MethodRecord> allMethods =
        existingMethods.stream()
            .flatMap(methodRecord -> registry.getCalledMethods(methodRecord).stream())
            .collect(Collectors.toSet());
    Set<MethodRecord> methodRecords =
        requests.stream()
            .map(
                name -> {
                  Optional<MethodRecord> optional =
                      allMethods.stream()
                          .filter(
                              methodRecord -> {
                                SignatureMatcher matcher =
                                    new SignatureMatcher(methodRecord.location.method);
                                return matcher.callableName.equals(name);
                              })
                          .findFirst();
                  return optional.orElse(null);
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    calls.get(calls.size() - 1).addAll(methodRecords);
  }
}
