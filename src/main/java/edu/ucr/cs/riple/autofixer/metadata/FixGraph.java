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
        if (candidate.fix.index.equals(fix.index)
            && candidate.fix.method.equals(fix.method)
            && candidate.fix.className.equals(fix.className)) {
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

  public Node find(String index, String method, String className) {
    int hash = Objects.hash(index, method, className);
    if (nodes.containsKey(hash)) {
      for (Node candidate : nodes.get(hash)) {
        if (candidate.fix.index.equals(index)
            && candidate.fix.method.equals(method)
            && candidate.fix.className.equals(className)) {
          return candidate;
        }
      }
    }
    return null;
  }

  public void findGroups(UsageTracker tracker) {
    for (List<Node> nodesSet : nodes.values()) {
      for (Node node : nodesSet) {

        //        List<Node> neighbors = destToSrc.getUsage()
      }
    }
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
