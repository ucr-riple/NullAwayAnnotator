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

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.Config;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Container class which loads its content from a file in TSV format. For faster retrieval, it
 * stores its content in a {@link com.google.common.collect.ImmutableMultimap} where the key is the
 * hash of the item and the value is the item itself. For faster retrieval, if the anticipated hash
 * is known, {@link Registry#findNodesWithHashHint} can be used, otherwise use {@link
 * Registry#findNodes}. If subclasses need to initialize some data before loading the file, they
 * must call {@link Registry#setup()}. Please note that this class anticipates that the file exits
 * at the given paths and does not attempt to create it. Before creating an instance, please make
 * sure that the file exists.
 */
public abstract class Registry<T> {

  /**
   * Contents, every element is mapped to it's computed hash, note that two different items can have
   * an identical hash, therefore it is of type {@link ImmutableMultimap} ({@link
   * java.util.HashMap}) to hold both items.
   */
  protected final ImmutableMultimap<Integer, T> contents;
  /** Annotator config. */
  protected final Config config;

  /**
   * Constructor for this container. Once this constructor is invoked, all data will be loaded from
   * the file.
   *
   * @param config Annotator config.
   * @param path Path to the file containing the data.
   */
  public Registry(Config config, Path path) {
    this.config = config;
    ImmutableMultimap.Builder<Integer, T> builder = ImmutableMultimap.builder();
    setup();
    try {
      populateContent(path, builder);
    } catch (IOException e) {
      throw new RuntimeException("Error happened while loading content of file: " + path, e);
    }
    this.contents = builder.build();
  }

  /**
   * Constructor for this container. Contents are accumulated from multiple sources. Once this
   * constructor is invoked, all data will be loaded from the file.
   *
   * @param config Annotator config.
   * @param paths Paths to all files containing data.
   */
  public Registry(Config config, ImmutableSet<Path> paths) {
    this.config = config;
    ImmutableMultimap.Builder<Integer, T> builder = ImmutableMultimap.builder();
    setup();
    paths.forEach(
        path -> {
          try {
            populateContent(path, builder);
          } catch (IOException e) {
            throw new RuntimeException("Error happened while loading content of file: " + path, e);
          }
        });
    this.contents = builder.build();
  }

  /**
   * Subclasses can override this method to perform any initialization before loading data from the
   * file.
   */
  protected void setup() {}

  /**
   * Loads data existing in the given path, to the given builder.
   *
   * @param path Path to the file containing data.
   * @throws IOException if file not is found.
   */
  protected void populateContent(Path path, ImmutableMultimap.Builder<Integer, T> builder)
      throws IOException {
    try (BufferedReader reader =
        Files.newBufferedReader(path.toFile().toPath(), Charset.defaultCharset())) {
      Builder<T> recordBuilder = getBuilder();
      String line = reader.readLine();
      if (line != null) {
        // Skip header
        line = reader.readLine();
      }
      while (line != null) {
        T node = recordBuilder.build(line.split("\t"));
        if (node != null) {
          builder.put(node.hashCode(), node);
        }
        line = reader.readLine();
      }
    }
  }

  /**
   * Returns the corresponding {@link Builder} for this registry which can make a record instance of
   * type {@link T} from a row in the given TSV file.
   *
   * @return {@link Builder} for this registry.
   */
  protected abstract Builder<T> getBuilder();

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
   * Retrieves stream of nodes which holds the passed predicate. This method is expected to be
   * significantly slower than {@link Registry#findNodesWithHashHint}, if the anticipated hash is
   * known, please use {@link Registry#findNodesWithHashHint}.
   *
   * @param c Predicate.
   * @return Corresponding stream of {@code T}.
   */
  protected Stream<T> findNodes(Predicate<T> c) {
    return contents.values().stream().filter(c);
  }

  /**
   * Builder interface for creating {@link Registry} items. Builders with this interface can make a
   * registry record of type {@link T} from a row of a TSV file.
   *
   * @param <T> Type of the registry record.
   */
  public interface Builder<T> {
    /**
     * Builds a registry record of type {@link T} from a row of a TSV file.
     *
     * @param values Row of a TSV file.
     * @return Registry record of type {@link T}.
     */
    T build(String[] values);
  }
}
