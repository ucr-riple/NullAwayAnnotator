package edu.ucr.cs.riple.autofixer.metadata;

import edu.ucr.cs.riple.injector.Fix;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class FixGraph {
  public final HashMap<Integer, List<Node>> nodes;
  private HashMap<Integer, List<Node>> groups;

  public FixGraph() {
    nodes = new HashMap<>();
  }

  private boolean fixMatcher(Fix fix, Fix other) {
    return fix.className.equals(other.className)
        && fix.method.equals(other.method)
        && fix.index.equals(other.index)
        && fix.param.equals(other.param);
  }

  public Node findOrCreate(Fix fix) {
    int hash = Objects.hash(fix.index, fix.method, fix.className);
    if (nodes.containsKey(hash)) {
      for (Node candidate : nodes.get(hash)) {
        if (fixMatcher(candidate.fix, fix)) {
          return candidate;
        }
      }
      Node newNode = new Node(fix);
      nodes.get(hash).add(newNode);
      return newNode;
    }
    Node newNode = new Node(fix);
    List<Node> newList = new ArrayList<>();
    newList.add(newNode);
    nodes.put(hash, newList);
    return newNode;
  }

  public Node find(Fix fix) {
    int hash = Objects.hash(fix.index, fix.method, fix.className);
    if (nodes.containsKey(hash)) {
      for (Node candidate : nodes.get(hash)) {
        if (fixMatcher(candidate.fix, fix)) {
          return candidate;
        }
      }
    }
    return null;
  }

  @SuppressWarnings("All")
  public void findGroups(UsageTracker tracker) {
    groups = new HashMap<>();
    List<Node> allNodes = new ArrayList<>();
    for (List<Node> nodesSet : nodes.values()) {
      allNodes.addAll(nodesSet);
    }
    for (int i = 0; i < allNodes.size(); i++) {
      Node node = allNodes.get(i);
      node.id = i;
      node.usages = tracker.getUsage(node.fix);
    }
    int size = allNodes.size();
    LinkedList<Integer>[] adj = new LinkedList[size];
    for (int i = 0; i < size; ++i) {
      adj[i] = new LinkedList<>();
    }
    for (Node node : allNodes) {
      for (Node other : allNodes) {
        if (node.hasConflictInUsage(other)) {
          node.neighbors.add(other);
          other.neighbors.add(node);
          adj[node.id].add(other.id);
        }
      }
    }
    colorGraph(adj, allNodes, size);
  }

  private void colorGraph(LinkedList<Integer>[] adj, List<Node> allNodes, int v) {
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
        ArrayList<Node> newList = new ArrayList<>();
        newList.add(allNodes.get(i));
        groups.put(result[i], newList);
      } else {
        groups.get(result[i]).add(allNodes.get(i));
      }
    }
    System.out.println("FOUND: " + groups.size() + " number of groups");
  }

  public static class Node {
    public final Fix fix;
    public int referred;
    public int effect;
    public int id;
    List<Node> neighbors;
    List<UsageTracker.Usage> usages;

    private Node(Fix fix) {
      this.fix = fix;
      neighbors = new ArrayList<>();
    }

    @Override
    public int hashCode() {
      return Objects.hash(fix.index, fix.className, fix.method);
    }

    public boolean hasConflictInUsage(Node node) {
      for (UsageTracker.Usage usage : this.usages) {
        if (node.usages.contains(usage)) {
          return true;
        }
      }
      return false;
    }
  }
}
