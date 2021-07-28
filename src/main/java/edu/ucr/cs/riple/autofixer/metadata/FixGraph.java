package edu.ucr.cs.riple.autofixer.metadata;

import edu.ucr.cs.riple.injector.Fix;
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

  public Node findOrCreate(Fix fix) {
    int hash = Objects.hash(fix.index, fix.method, fix.className);
    if (nodes.containsKey(hash)) {
      for (Node candidate : nodes.get(hash)) {
        if (candidate.fix.equals(fix)) {
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
        if (candidate.fix.equals(fix)) {
          return candidate;
        }
      }
    }
    return null;
  }

  public List<List<Node>> findGroups(UsageTracker tracker) {
    return null;
  }

  public static class Node {
    public final Fix fix;
    public int referred;
    public int effect;
    List<Node> neighbors;

    private Node(Fix fix) {
      this.fix = fix;
      neighbors = new ArrayList<>();
    }

    @Override
    public int hashCode() {
      return Objects.hash(fix.index, fix.className, fix.method);
    }
  }
}
