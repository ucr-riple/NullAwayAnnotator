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

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The Conflict Graph for the exploring process. In this graph vertices are {@link Node} and there
 * is an edge between two nodes, if they share a potentially impacted region.
 */
public class ConflictGraph {

  /** Nodes in this graph */
  public final Multimap<Integer, Node> nodes;
  /**
   * Groups in this graph, nodes which does not have any conflict in regions will in the same group.
   * Please note that this is a graph coloring problem, set of groups is calculated using a greedy
   * algorithm can may not be optimal.
   */
  private final HashMap<Integer, Set<Node>> groups;

  public ConflictGraph() {
    nodes = MultimapBuilder.hashKeys().arrayListValues().build();
    groups = new HashMap<>();
  }

  /**
   * Adds a node to the list of vertices.
   *
   * @param fix Corresponding fix for node.
   * @return The created node.
   */
  public Node addNodeToVertices(Fix fix) {
    Node node = new Node(fix);
    nodes.put(Node.getHash(fix), node);
    return node;
  }

  /**
   * Colors the graph based on edges, no two vertices which there is an edge connecting them will be
   * in the same group. A greedy algorithm is used to find the solution.
   */
  // TODO: Remove SuppressWarnings below later.
  @SuppressWarnings("JdkObsolete")
  public void findGroups() {
    this.groups.clear();
    Collection<Node> allNodes = nodes.values();
    int counter = 0;
    for (Node node : allNodes) {
      node.id = counter++;
    }
    int size = allNodes.size();
    LinkedList<Integer>[] adj = new LinkedList[size];
    for (int i = 0; i < size; ++i) {
      adj[i] = new LinkedList<>();
    }
    for (Node node : allNodes) {
      for (Node other : allNodes) {
        if (node.equals(other)) {
          continue;
        }
        if (node.hasConflictInRegions(other)) {
          adj[node.id].add(other.id);
        }
      }
    }
    colorGraph(adj, allNodes);
  }

  /**
   * Performs the actual coloring.
   *
   * @param adj Martic of adjancey.
   * @param nodes Nodes in the graph.
   */
  private void colorGraph(LinkedList<Integer>[] adj, Collection<Node> nodes) {
    int v = nodes.size();
    List<Node> allNodes = new ArrayList<>(nodes);
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
        if (available[cr]) {
          break;
        }
      }
      result[u] = cr;
      Arrays.fill(available, true);
    }
    for (int i = 0; i < result.length; i++) {
      if (!groups.containsKey(result[i])) {
        Set<Node> newList = new HashSet<>();
        newList.add(allNodes.get(i));
        groups.put(result[i], newList);
      } else {
        groups.get(result[i]).add(allNodes.get(i));
      }
    }
  }

  /**
   * Returns the collected groups.
   *
   * @return CollectionGroups.
   */
  public Collection<Set<Node>> getGroups() {
    return groups.values();
  }

  /**
   * Returns all nodes values as stream.
   *
   * @return returns all nodes in the graph.
   */
  public Stream<Node> getNodes() {
    return nodes.values().stream();
  }

  /** Clears all nodes and groups. */
  public void clear() {
    nodes.clear();
    groups.clear();
  }

  /**
   * Checks if graph has any node.
   *
   * @return true, if the graph is empty.
   */
  public boolean isEmpty() {
    return nodes.isEmpty();
  }
}
