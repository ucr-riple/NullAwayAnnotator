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

package edu.ucr.cs.riple.core.registries.method;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.registries.region.MethodRegionRegistry;
import edu.ucr.cs.riple.core.registries.region.RegionRecord;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** A type based call graph that maps a method to its callers. */
public class TypeBasedCallGraph {

  /**
   * A map from method to its callers. This relation is context insensitive and only contains direct
   * type based calls.
   */
  private final ImmutableMap<MethodRecord, ImmutableSet<MethodRecord>> map;

  /**
   * Creates a type based call graph from a method registry and a method region registry.
   *
   * @param methodRegistry the method registry.
   * @param registry the method region registry.
   */
  public TypeBasedCallGraph(MethodRegistry methodRegistry, MethodRegionRegistry registry) {
    Map<MethodRecord, Set<MethodRecord>> holder = new HashMap<>();
    for (RegionRecord record : registry.getRecords()) {
      MethodRecord caller =
          methodRegistry.findMethodByName(record.region.clazz, record.region.member);
      if (caller == null) {
        continue;
      }
      MethodRecord callee = methodRegistry.findMethodByName(record.encClass, record.member);
      if (callee == null) {
        continue;
      }
      holder.putIfAbsent(caller, new HashSet<>());
      holder.get(caller).add(callee);
    }
    ImmutableMap.Builder<MethodRecord, ImmutableSet<MethodRecord>> builder = ImmutableMap.builder();
    for (Map.Entry<MethodRecord, Set<MethodRecord>> entry : holder.entrySet()) {
      builder.put(entry.getKey(), ImmutableSet.copyOf(entry.getValue()));
    }
    this.map = builder.build();
  }

  /**
   * Returns the set of callers of the given method.
   *
   * @param callee the method to find its callers.
   * @return the set of callers of the given method.
   */
  public ImmutableSet<MethodRecord> getCallers(MethodRecord callee) {
    return map.getOrDefault(callee, ImmutableSet.of());
  }
}
