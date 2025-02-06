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

import edu.ucr.cs.riple.core.registries.method.MethodRecord;
import edu.ucr.cs.riple.core.registries.region.Region;
import edu.ucr.cs.riple.core.util.ASTParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class InvocationRecord {

  /**
   * A list of sets of method records. Each set contains the methods that are called at the same
   * depth.
   */
  private final List<Set<MethodRecord>> calls;

  public InvocationRecord() {
    this.calls = new ArrayList<>(3);
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
}
