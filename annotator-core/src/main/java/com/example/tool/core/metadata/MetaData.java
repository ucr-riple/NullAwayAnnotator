/*
 * MIT License
 *
 * Copyright (c) 2020 anonymous
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

package com.example.tool.core.metadata;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.example.tool.core.Config;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Container class which loads its content from a file in TSV format. For faster retrieval, it uses
 * hashing techniques to store data. For faster retrieval, the anticipated hash must be passed to
 * search methods.
 */
public abstract class MetaData<T> {

  /**
   * Contents, every element is mapped to it's computed hash, note that two different items can have
   * an identical hash, therefore it is of type {@link LinkedHashMultimap} (not HashMap) to hold
   * both items.
   */
  protected final LinkedHashMultimap<Integer, T> contents;

  /** Annotator config. */
  protected final Config config;

  /**
   * Constructor for this container. Once this constructor is invoked, all data will be loaded from
   * the file.
   *
   * @param config Annotator config.
   * @param path Path to the file containing the data.
   */
  public MetaData(Config config, Path path) {
    contents = LinkedHashMultimap.create();
    this.config = config;
    setup();
    try {
      fillNodes(path);
    } catch (IOException e) {
      throw new RuntimeException("Error happened while loading content of file: " + path, e);
    }
  }

  /**
   * Constructor for this container. Contents are accumulated from multiple sources. Once this
   * constructor is invoked, all data will be loaded from the file.
   *
   * @param config Annotator config.
   * @param paths Paths to all files containing data.
   */
  public MetaData(Config config, ImmutableSet<Path> paths) {
    contents = LinkedHashMultimap.create();
    this.config = config;
    setup();
    paths.forEach(
        path -> {
          try {
            fillNodes(path);
          } catch (IOException e) {
            throw new RuntimeException("Error happened while loading content of file: " + path, e);
          }
        });
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
    try (BufferedReader reader =
        Files.newBufferedReader(path.toFile().toPath(), Charset.defaultCharset())) {
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
    }
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