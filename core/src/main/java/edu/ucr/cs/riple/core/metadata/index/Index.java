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

package edu.ucr.cs.riple.core.metadata.index;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import edu.ucr.cs.riple.core.metadata.trackers.Region;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Indexes contents of type {@link Enclosed} based on the computed hash for fast retrieval. This
 * data structure loads its data from a file at the given path.
 */
public class Index<T extends Enclosed> {

  /**
   * Contents of the index. Items can have a duplicate hashes, therefore a {@link Multimap} is used.
   */
  private final Multimap<Integer, T> items;
  /** Factory instance. */
  private final Factory<T> factory;
  /** Path to the file to load the content from. */
  private final Path path;
  /** Type of index. Used to compute hash. */
  private final Index.Type type;
  /** Total number of items. */
  public int total;

  /** Index type. */
  public enum Type {
    BY_METHOD,
    BY_CLASS
  }

  /**
   * Creates an instance of Index.
   *
   * @param path Path to the file to load the data from.
   * @param type Type of index.
   * @param factory Factory to create instances from file lines.
   */
  public Index(Path path, Index.Type type, Factory<T> factory) {
    this.type = type;
    this.path = path;
    this.items = MultimapBuilder.hashKeys().arrayListValues().build();
    this.factory = factory;
    this.total = 0;
  }

  /** Starts the reading and index process. */
  public void index() {
    items.clear();
    try (BufferedReader br = Files.newBufferedReader(this.path, UTF_8)) {
      String line = br.readLine();
      if (line != null) {
        line = br.readLine();
      }
      while (line != null) {
        T item = factory.build(line.split("\t"));
        total++;
        int hash;
        if (type.equals(Index.Type.BY_CLASS)) {
          hash = Objects.hash(item.encClass());
        } else {
          hash = Objects.hash(item.encClass(), item.encMethod());
        }
        items.put(hash, item);
        line = br.readLine();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Returns all contents which are enclosed by the given class.
   *
   * @param clazz Fully qualified name of the class.
   * @return Stored contents that are enclosed by the given class.
   */
  public Collection<T> getByClass(String clazz) {
    return items.get(Objects.hash(clazz)).stream()
        .filter(item -> item.encClass().equals(clazz))
        .collect(Collectors.toList());
  }

  /**
   * Returns all contents which are enclosed by the given class and method.
   *
   * @param clazz Fully qualified name of the class.
   * @param method Method signature.
   * @return Stored contents that are enclosed by the given class and method.
   */
  public Collection<T> getByMethod(String clazz, String method) {
    return items.get(Objects.hash(clazz, method)).stream()
        .filter(item -> item.encClass().equals(clazz) && item.encMethod().equals(method))
        .collect(Collectors.toList());
  }

  /**
   * Returns all values.
   *
   * @return Collection of all values.
   */
  public Collection<T> values() {
    return items.values();
  }

  /**
   * Returns all items regions that holds the given predicate.
   *
   * @param predicate Predicate provided by caller.
   * @return Set of regions.
   */
  public Set<Region> getRegionsOfMatchingItems(Predicate<T> predicate) {
    return values().stream().filter(predicate).map(t -> t.region).collect(Collectors.toSet());
  }
}
