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

package edu.ucr.cs.riple.core.metadata;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public abstract class AbstractRelation<T> {
  HashMap<Integer, List<T>> idHash;

  public AbstractRelation(Path path) {
    setup();
    try {
      fillNodes(path);
    } catch (IOException e) {
      throw new RuntimeException("Did not found file at" + path, e);
    }
  }

  protected void setup() {
    idHash = new HashMap<>();
  }

  protected void fillNodes(Path path) throws IOException {
    BufferedReader reader;
    reader = new BufferedReader(new FileReader(path.toFile()));
    String line = reader.readLine();
    if (line != null) line = reader.readLine();
    while (line != null) {
      T node = addNodeByLine(line.split("\t"));
      Integer hash = node.hashCode();
      if (idHash.containsKey(hash)) {
        List<T> localList = idHash.get(hash);
        if (!localList.contains(node)) {
          localList.add(node);
        }
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

  public interface Comparator<T> {
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

  protected List<T> findAllNodes(Comparator<T> c, String... keys) {
    List<T> nodes = new ArrayList<>();
    List<T> candidateIds = idHash.get(Arrays.hashCode(keys));
    if (candidateIds == null) {
      return nodes;
    }
    for (T candidate : candidateIds) {
      if (c.matches(candidate)) {
        nodes.add(candidate);
      }
    }
    return nodes;
  }
}
