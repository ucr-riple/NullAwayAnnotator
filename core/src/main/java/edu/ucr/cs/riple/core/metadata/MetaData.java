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

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Container class which loads its content from a file in TSV format. For faster retrieval, it uses
 * hashing techniques to store data. Fir faster retrieval, the anticipated hash must be passed to
 * search methods.
 *
 * @param <T> Any class which implements {@link Hashable}.
 */
public abstract class MetaData<T extends Hashable> {

  /**
   * Contents, every element is mapped to it's computed hash, note that two different items can have
   * an identical hash, therefore it s of type {@link Multimap} (not HashMap) to hold both items.
   */
  protected final Multimap<Integer, T> contents;

  /**
   * Constructor for this container. Once this constructor is invoked, all data will be loaded from
   * the file.
   *
   * @param path Path to the file containing the data.
   */
  public MetaData(Path path) {
    contents = MultimapBuilder.hashKeys().arrayListValues().build();
    setup();
    try {
      fillNodes(path);
    } catch (IOException e) {
      throw new RuntimeException("Error happened while loading content of file: " + path, e);
    }
  }

  /**
   * Subclasses can override this method to perform eny initialization before loading data from the
   * file.
   */
  protected void setup() {}

  /**
   * Loads data to contents.
   *
   * @param path Path to the file containing data.
   * @throws IOException if file not is found.
   */
  protected void fillNodes(Path path) throws IOException {
    BufferedReader reader;
    reader = new BufferedReader(new FileReader(path.toFile()));
    String line = reader.readLine();
    if (line != null) {
      line = reader.readLine();
    }
    while (line != null) {
      T node = addNodeByLine(line.split("\t"));
      if (node != null) {
        contents.put(node.hashCode(), node);
      }
      line = reader.readLine();
    }
    reader.close();
  }

  /**
   * Creates an instance of {@code T} corresponding to the values in a row. This method is called on
   * every row of the loaded file.
   *
   * @param values Values in a row.
   * @return Instance of {@code T}.
   */
  protected abstract T addNodeByLine(String[] values);

  /**
   * Retrieves node which holds the passed predicate. It uses the given hash for faster retrieval.
   *
   * @param c Predicate.
   * @param hash Expected hash
   * @return Corresponding {@code T}.
   */
  protected T findNodeWithHashHint(Predicate<T> c, int hash) {
    return findNodesWithHashHint(c, hash).findFirst().orElse(null);
  }

  /**
   * Retrieves stream of nodes which holds the passed predicate. It uses the given hash for faster
   * retrieval.
   *
   * @param c Predicate.
   * @param hash Expected hash
   * @return Corresponding stream of {@code T}.
   */
  protected Stream<T> findNodesWithHashHint(Predicate<T> c, int hash) {
    return contents.get(hash).stream().filter(c);
  }

  /**
   * Retrieves stream of nodes which holds the passed predicate. This method does not hash and is
   * significantly slower than {@link MetaData#findNodesWithHashHint(Predicate, int)};
   *
   * @param c Predicate.
   * @return Corresponding stream of {@code T}.
   */
  protected Stream<T> findNodes(Predicate<T> c) {
    return contents.values().stream().filter(c);
  }
}
