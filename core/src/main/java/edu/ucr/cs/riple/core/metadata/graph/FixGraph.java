/*
 * MIT License
 *
 * Copyright (c) 2020 Nima Karimipour
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

package edu.ucr.cs.riple.core.metadata.graph;

import edu.ucr.cs.riple.injector.Location;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class FixGraph<T extends Node> {
  public final HashMap<Integer, Set<T>> nodes;
  private final HashMap<Integer, Set<T>> groups;
  private final Factory<T> factory;

  public FixGraph(Factory<T> factory) {
    nodes = new HashMap<>();
    groups = new HashMap<>();
    this.factory = factory;
  }

  public T findOrCreate(Location fix) {
    int hash = Node.getHash(fix);
    if (nodes.containsKey(hash)) {
      for (T candidate : nodes.get(hash)) {
        if (candidate.root.equals(fix)) {
          return candidate;
        }
      }
      T newNode = factory.build(fix);
      nodes.get(hash).add(newNode);
      return newNode;
    }
    T newNode = factory.build(fix);
    Set<T> newSet = new HashSet<>();
    newSet.add(newNode);
    nodes.put(hash, newSet);
    return newNode;
  }

  @SuppressWarnings("ALL")
  public void remove(Location fix) {
    int hash = Node.getHash(fix);
    T toRemove = null;
    if (nodes.containsKey(hash)) {
      for (T candidate : nodes.get(hash)) {
        if (candidate.root.equals(fix)) {
          toRemove = candidate;
          break;
        }
      }
    }
    nodes.remove(toRemove);
  }

  public void findGroups(boolean optimized) {
    this.groups.clear();
    List<T> allNodes = getAllNodes();
    final int[] id = {0};
    allNodes.forEach(node -> node.id = id[0]++);
    if (!optimized) {
      allNodes.forEach(t -> groups.put(t.id, Collections.singleton(t)));
      return;
    }
    int size = allNodes.size();
    LinkedList<Integer>[] adj = new LinkedList[size];
    for (int i = 0; i < size; ++i) {
      adj[i] = new LinkedList<>();
    }
    for (T node : allNodes) {
      for (T other : allNodes) {
        if (node.equals(other)) {
          continue;
        }
        if (node.hasConflictInUsage(other)) {
          adj[node.id].add(other.id);
        }
      }
    }
    colorGraph(adj, allNodes, size);
  }

  private void colorGraph(LinkedList<Integer>[] adj, List<T> allNodes, int v) {
    int[] result = new int[v];
    Arrays.fill(result, -1);
    result[0] = 0;
    boolean[] available = new boolean[v];
    Arrays.fill(available, true);
    for (int u = 1; u < v; u++) {
      for (int i : adj[u]) {
        if (result[i] != -1) {
          available[result[i]] = false;
        }
      }
      int cr;
      for (cr = 0; cr < v; cr++) {
        if (available[cr]) break;
      }
      result[u] = cr;
      Arrays.fill(available, true);
    }
    for (int i = 0; i < result.length; i++) {
      if (!groups.containsKey(result[i])) {
        Set<T> newList = new HashSet<>();
        newList.add(allNodes.get(i));
        groups.put(result[i], newList);
      } else {
        groups.get(result[i]).add(allNodes.get(i));
      }
    }
  }

  public HashMap<Integer, Set<T>> getGroups() {
    return groups;
  }

  public List<T> getAllNodes() {
    List<T> ans = new ArrayList<>();
    nodes.values().forEach(ans::addAll);
    return ans;
  }

  public void clear() {
    nodes.clear();
    groups.clear();
  }
}
