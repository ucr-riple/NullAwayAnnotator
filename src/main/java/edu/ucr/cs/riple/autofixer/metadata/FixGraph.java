package edu.ucr.cs.riple.autofixer.metadata;

import edu.ucr.cs.riple.injector.Fix;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

  public void updateUsages(UsageTracker tracker) {
    List<Node> allNodes = new ArrayList<>();
    for (List<Node> nodesSet : nodes.values()) {
      allNodes.addAll(nodesSet);
    }
    for (int i = 0; i < allNodes.size(); i++) {
      Node node = allNodes.get(i);
      node.id = i;
      node.updateUsages(tracker.getUsage(node.fix));
    }
  }

  @SuppressWarnings("All")
  public void findGroups() {
    groups = new HashMap<>();
    List<Node> allNodes = new ArrayList<>();
    for (List<Node> nodesSet : nodes.values()) {
      allNodes.addAll(nodesSet);
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
        if (node.hasConflictInUsage(other)) {
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

  public HashMap<Integer, List<Node>> getGroups() {
    return groups;
  }

  public static class Node {
    public final Fix fix;
    public final Set<UsageTracker.Usage> usages;
    public final Set<String> classes;
    public int referred;
    public int effect;
    public int id;
    public boolean isDangling;

    private Node(Fix fix) {
      this.fix = fix;
      isDangling = false;
      this.usages = new HashSet<>();
      this.classes = new HashSet<>();
    }

    @Override
    public int hashCode() {
      return Objects.hash(fix.index, fix.className, fix.method);
    }

    public void updateUsages(Set<UsageTracker.Usage> usages) {
      this.usages.addAll(usages);
      this.classes.addAll(usages.stream().map(usage -> usage.clazz).collect(Collectors.toSet()));
      for (UsageTracker.Usage usage : usages) {
        if (usage.method == null || usage.method.equals("null")) {
          isDangling = true;
          break;
        }
      }
    }

    public boolean hasConflictInUsage(Node node) {
      if (node.isDangling || this.isDangling) {
        return !Collections.disjoint(node.classes, this.classes);
      }
      return !Collections.disjoint(node.usages, this.usages);
    }

    @Override
    public String toString() {
      return "Node{"
          + "fix=["
          + fix.method
          + " "
          + fix.className
          + " "
          + fix.param
          + " "
          + fix.location
          + "]"
          + ", id="
          + id
          + ", effect="
          + effect
          + ", referred="
          + referred
          + ", isDangling="
          + isDangling
          + '}';
    }
  }
}
