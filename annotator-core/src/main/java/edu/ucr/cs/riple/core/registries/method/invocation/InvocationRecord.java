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

package edu.ucr.cs.riple.core.registries.method.invocation;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.registries.method.MethodRecord;
import edu.ucr.cs.riple.core.registries.region.Region;
import edu.ucr.cs.riple.core.util.ASTParser;
import edu.ucr.cs.riple.injector.util.SignatureMatcher;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

  /**
   * Contains third-party method declarations not found in the source code. These methods will be
   * provided to the model as plain-text descriptions only.
   */
  private final Set<String> thirdPartyLibs;

  /** Parser to parse the AST of the methods and retrieve their source code */
  private final ASTParser parser;

  /** The logger instance. */
  private static final Logger logger = LogManager.getLogger(InvocationRecord.class);

  public InvocationRecord(InvocationRecordRegistry registry) {
    this.calls = new ArrayList<>(3);
    this.registry = registry;
    this.thirdPartyLibs = new HashSet<>();
    this.parser = registry.getParser();
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
   * @return a prompt for the call graph.
   */
  public String constructCallGraphContext() {
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
    if (!thirdPartyLibs.isEmpty()) {
      prompt.append(
          "These methods are from third party code and the source code is not available, consider the default implementation you know about them.\n");
      for (String lib : thirdPartyLibs) {
        prompt.append(lib).append("\n");
      }
      prompt.append(
          "There is no source code for these methods, they have the default implementation, if you ask about them, I cannot provide you with any information about them, make your best guess.\n");
    }
    return prompt.toString();
  }

  /**
   * Returns the root of the invocation record. This is used to identify the starting point of the
   * call graph.
   *
   * @return the root of the invocation record, which is null in this implementation.
   */
  public String getRoot() {
    MethodRecord root = calls.get(0).iterator().next();
    return parser.getRegionSourceCode(new Region(root.location.clazz, root.location.method))
        .content;
  }

  /**
   * Adds the requested methods to the record on the top of the stack. These methods are not present
   * in the invocation record and are only called within methods.
   *
   * @param requests Names of the requested methods.
   * @return true if the requested methods are added to the record, false otherwise.
   */
  public boolean addRequestedMethodsByNames(ImmutableSet<String> requests) {
    logger.trace("Adding requested methods to invocation record: {}", requests);
    Set<MethodRecord> existingMethods =
        calls.stream().flatMap(Set::stream).collect(Collectors.toSet());
    Set<MethodRecord> allMethods =
        existingMethods.stream()
            .flatMap(methodRecord -> registry.getCalledMethods(methodRecord).stream())
            .collect(Collectors.toSet());
    Set<MethodRecord> methodRecords = new HashSet<>();
    for (String request : requests) {
      // if for any reason the requested method name contains receiver, just pick the method:
      if (request.contains(".")) {
        request = request.substring(request.lastIndexOf(".") + 1);
      }
      String finalRequest = request;
      Optional<MethodRecord> optional =
          allMethods.stream()
              .filter(
                  record -> {
                    SignatureMatcher matcher = new SignatureMatcher(record.location.method);
                    return matcher.callableName.equals(finalRequest);
                  })
              .findFirst();
      optional.ifPresentOrElse(methodRecords::add, () -> thirdPartyLibs.add(finalRequest));
    }
    int total = calls.stream().mapToInt(Set::size).sum();
    calls.get(calls.size() - 1).addAll(methodRecords);
    return total < calls.stream().mapToInt(Set::size).sum();
  }
}
