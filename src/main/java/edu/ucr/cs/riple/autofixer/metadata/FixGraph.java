package edu.ucr.cs.riple.autofixer.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class FixGraph {
  public final HashMap<Integer, List<Node>> nodes;
  private List<List<Node>> groups;

  public FixGraph() {
    nodes = new HashMap<>();
  }

  public Node findOrCreate(String index, String method, String className) {
    int hash = Objects.hash(index, method, className);
    if (nodes.containsKey(hash)) {
      for (Node candidate : nodes.get(hash)) {
        if (candidate.index.equals(index)
            && candidate.method.equals(method)
            && candidate.className.equals(className)) {
          return candidate;
        }
      }
      Node newNode = new Node(index, method, className);
      nodes.get(hash).add(newNode);
      return newNode;
    }
    Node newNode = new Node(index, method, className);
    List<Node> newList = new ArrayList<>();
    newList.add(newNode);
    nodes.put(hash, newList);
    return newNode;
  }

  public Node find(String index, String method, String className) {
    int hash = Objects.hash(index, method, className);
    if (nodes.containsKey(hash)) {
      for (Node candidate : nodes.get(hash)) {
        if (candidate.index.equals(index)
            && candidate.method.equals(method)
            && candidate.className.equals(className)) {
          return candidate;
        }
      }
    }
    return null;
  }

  public void findGroups(UsageTracker srcToDest, UsageTracker destToSrc) {
    for (List<Node> nodesSet : nodes.values()) {
      for (Node node : nodesSet) {}
    }
  }

  public static class Node {
    public String index;
    public String method;
    public String className;
    public int referred;
    public int effect;
    List<Node> neighbors;

    private Node(String index, String method, String className) {
      this.index = index;
      this.method = method;
      this.className = className;
      neighbors = new ArrayList<>();
    }

    @Override
    public int hashCode() {
      return Objects.hash(index, className, method);
    }
  }
}
