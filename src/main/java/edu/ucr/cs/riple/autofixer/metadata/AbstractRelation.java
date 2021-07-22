package edu.ucr.cs.riple.autofixer.metadata;

import edu.ucr.cs.riple.autofixer.nullaway.Writer;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public abstract class AbstractRelation<T> {
  HashMap<Integer, List<T>> idHash;

  public AbstractRelation(String filePath) {
    setup();
    try {
      fillNodes(filePath);
    } catch (IOException e) {
      System.out.println("Not found: " + filePath);
    }
  }

  protected void setup() {
    idHash = new HashMap<>();
  }

  protected void fillNodes(String filePath) throws IOException {
    BufferedReader reader;
    reader = new BufferedReader(new FileReader(filePath));
    String line = reader.readLine();
    if (line != null) line = reader.readLine();
    String delimiter = Writer.getDelimiterRegex();
    while (line != null) {
      T node = addNodeByLine(line.split(delimiter));
      Integer hash = node.hashCode();
      if (idHash.containsKey(hash)) {
        idHash.get(hash).add(node);
      } else {
        List<T> singleHash = new ArrayList<>();
        singleHash.add(node);
        idHash.put(hash, singleHash);
      }
      line = reader.readLine();
    }
    reader.close();
  }

  protected abstract T addNodeByLine(String[] values);

  interface Comparator<T> {
    boolean matches(T candidate);
  }

  protected T findNode(Comparator<T> c, String... arguments) {
    T node = null;
    int hash = Arrays.hashCode(arguments);
    List<T> candidateIds = idHash.get(hash);
    if (candidateIds == null) {
      return null;
    }
    for (T candidate : candidateIds) {
      if (c.matches(candidate)) {
        node = candidate;
        break;
      }
    }
    return node;
  }

  protected List<T> findAllNodes(Comparator<T> c, String... arguments) {
    int hash = Arrays.hashCode(arguments);
    List<T> candidateIds = idHash.get(hash);
    if (candidateIds == null) {
      return null;
    }
    List<T> nodes = new ArrayList<>();
    for (T candidate : candidateIds) {
      if (c.equals(candidate)) {
        nodes.add(candidate);
      }
    }
    return nodes;
  }
}
