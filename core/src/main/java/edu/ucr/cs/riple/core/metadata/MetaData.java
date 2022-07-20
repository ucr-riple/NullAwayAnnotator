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
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public abstract class MetaData<T> {
  HashMap<Integer, List<T>> idHash;

  public MetaData(Path path) {
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
      if (node != null) {
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
      }
      line = reader.readLine();
    }
    reader.close();
  }

  protected abstract T addNodeByLine(String[] values);

  public interface Comparator<T> {
    boolean matches(T candidate);
  }

  protected T findNode(Comparator<T> c, String... keys) {
    if (keys.length == 0) {
      throw new RuntimeException("findNode needs keys to compute the hash and cannot be empty");
    }
    T node = null;
    int hash = Arrays.hashCode(keys);
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

  protected Stream<T> findNodes(Predicate<T> c, Object... keys) {
    if (keys.length == 0) {
      throw new RuntimeException("findAllNodes needs keys to compute the hash and cannot be empty");
    }
    List<T> candidateIds = idHash.get(Arrays.hashCode(keys));
    if (candidateIds == null) {
      return Stream.of();
    }
    return candidateIds.stream().filter(c);
  }

  public Stream<T> findAllNodes(Predicate<T> c) {
    return idHash.values().stream().flatMap(Collection::stream).filter(c);
  }
}
