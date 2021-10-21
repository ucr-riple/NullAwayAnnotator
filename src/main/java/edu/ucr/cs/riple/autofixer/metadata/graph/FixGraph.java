package edu.ucr.cs.riple.autofixer.metadata.graph;

import edu.ucr.cs.riple.autofixer.metadata.trackers.UsageTracker;
import edu.ucr.cs.riple.injector.Fix;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class FixGraph<T extends AbstractNode> {
  public final HashMap<Integer, Set<T>> nodes;

  private final HashMap<Integer, Set<T>> groups;
  private final Factory<T> factory;

  public FixGraph(Factory<T> factory) {
    nodes = new HashMap<>();
    groups = new HashMap<>();
    this.factory = factory;
  }

  public T findOrCreate(Fix fix) {
    int hash = AbstractNode.getHash(fix);
    if (nodes.containsKey(hash)) {
      for (T candidate : nodes.get(hash)) {
        if (candidate.fix.equals(fix)) {
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

  public T find(Fix fix) {
    int hash = AbstractNode.getHash(fix);
    if (nodes.containsKey(hash)) {
      for (T candidate : nodes.get(hash)) {
        if (candidate.fix.equals(fix)) {
          return candidate;
        }
      }
    }
    return null;
  }

  @SuppressWarnings("ALL")
  public void remove(Fix fix) {
    int hash = AbstractNode.getHash(fix);
    T toRemove = null;
    if (nodes.containsKey(hash)) {
      for (T candidate : nodes.get(hash)) {
        if (candidate.fix.equals(fix)) {
          toRemove = candidate;
          break;
        }
      }
    }
    nodes.remove(toRemove);
  }

  public void updateUsages(UsageTracker tracker) {
    getAllNodes().forEach(t -> t.updateUsages(tracker));
  }

  @SuppressWarnings("All")
  public void findGroups() {
    this.groups.clear();
    List<T> allNodes = getAllNodes();
    final int[] id = {0};
    allNodes.forEach(node -> node.id = id[0]++);
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
