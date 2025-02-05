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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.module.ModuleInfo;
import edu.ucr.cs.riple.core.registries.method.MethodRecord;
import edu.ucr.cs.riple.core.registries.method.MethodRegistry;
import edu.ucr.cs.riple.core.registries.region.RegionRecord;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InvocationRecordRegistry {

  /**
   * A map from method to its callers. This relation is context insensitive and only contains direct
   * type based calls.
   */
  private final ImmutableMap<MethodRecord, ImmutableSet<MethodRecord>> map;

  /** Method registry to find methods by name and class. */
  private final MethodRegistry methodRegistry;

  /**
   * Creates a type based invocation graph from a module.
   *
   * @param module the module to create the graph from.
   */
  public InvocationRecordRegistry(ModuleInfo module) {
    Map<MethodRecord, Set<MethodRecord>> holder = new HashMap<>();
    MethodRegistry methodRegistry = module.getMethodRegistry();
    for (RegionRecord record : module.getRegionRegistry().getMethodRegionRegistry().getRecords()) {
      MethodRecord caller =
          methodRegistry.findMethodByName(record.region.clazz, record.region.member);
      if (caller == null) {
        continue;
      }
      MethodRecord callee = methodRegistry.findMethodByName(record.encClass, record.member);
      if (callee == null) {
        continue;
      }
      holder.computeIfAbsent(callee, k -> new HashSet<>()).add(caller);
    }
    ImmutableMap.Builder<MethodRecord, ImmutableSet<MethodRecord>> builder = ImmutableMap.builder();
    for (Map.Entry<MethodRecord, Set<MethodRecord>> entry : holder.entrySet()) {
      builder.put(entry.getKey(), ImmutableSet.copyOf(entry.getValue()));
    }
    this.map = builder.build();
    this.methodRegistry = methodRegistry;
  }

  /**
   * Performs a BFS on the call graph to find the callers of the given method up to a depth of 3.
   *
   * @param clazz the class name of the target method.
   * @param member the member name of the target method.
   * @return A map from depth to the set of methods at that depth.
   */
  public InvocationRecord computeInvocationRecord(String clazz, String member) {
    MethodRecord current = methodRegistry.findMethodByName(clazz, member);
    Preconditions.checkArgument(
        current != null, String.format("Method not found: %s#%s", clazz, member));
    Deque<MethodRecord> deque = new ArrayDeque<>();
    int depth = 0;
    deque.add(current);
    InvocationRecord record = new InvocationRecord();
    while (!deque.isEmpty() && depth++ < 4) {
      Set<MethodRecord> calls = new HashSet<>();
      int size = deque.size();
      for (int i = 0; i < size; i++) {
        MethodRecord method = deque.poll();
        if (method == null) {
          continue;
        }
        calls.add(method);
        for (MethodRecord caller : map.getOrDefault(method, ImmutableSet.of())) {
          if (caller == null) {
            continue;
          }
          deque.add(caller);
        }
      }
      record.pushCallers(calls);
    }
    return record;
  }
}
