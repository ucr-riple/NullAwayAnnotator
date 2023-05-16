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

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import edu.ucr.cs.riple.core.Context;
import edu.ucr.cs.riple.core.metadata.trackers.Region;
import edu.ucr.cs.riple.core.module.ModuleInfo;
import edu.ucr.cs.riple.core.util.Utility;
import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Indexes {@link Error} instances based on the enclosing region. This data structure loads its data
 * from a file at the given path.
 */
public class Index {

  /** Contents of the index. */
  private final Multimap<Region, Error> items;
  /** ModuleInfo of the module which indexed errors are reported on. */
  private final ModuleInfo moduleInfo;
  /** Annotator context. */
  private final Context context;

  /** Creates an instance of Index. Contents are accumulated from multiple sources. */
  public Index(Context context, ModuleInfo moduleInfo) {
    this.context = context;
    this.moduleInfo = moduleInfo;
    this.items = MultimapBuilder.hashKeys().arrayListValues().build();
  }

  /** Starts the reading and index process. */
  public void index() {
    items.clear();
    Utility.readErrorsFromOutputDirectory(context, moduleInfo)
        .forEach(error -> items.put(error.getRegion(), error));
  }

  /**
   * Returns all contents which are enclosed by the given region.
   *
   * @param region Enclosing region.
   * @return Stored contents that are enclosed by the given region.
   */
  public Collection<Error> get(Region region) {
    return items.get(region);
  }

  /**
   * Returns all values.
   *
   * @return Collection of all values.
   */
  public Collection<Error> values() {
    return items.values();
  }

  /**
   * Returns all items regions that holds the given predicate.
   *
   * @param predicate Predicate provided by caller.
   * @return Set of regions.
   */
  public Set<Region> getRegionsOfMatchingItems(Predicate<Error> predicate) {
    return values().stream().filter(predicate).map(t -> t.region).collect(Collectors.toSet());
  }
}
