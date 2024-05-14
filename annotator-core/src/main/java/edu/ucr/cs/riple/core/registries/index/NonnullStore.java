/*
 * MIT License
 *
 * Copyright (c) 2023 Nima Karimipour
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

package edu.ucr.cs.riple.core.registries.index;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.Context;
import edu.ucr.cs.riple.core.module.ModuleConfiguration;
import edu.ucr.cs.riple.core.registries.Registry;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.scanner.Serializer;

/**
 * Structure for storing location of elements with explicit {@code @Nonnull} annotations. Used to
 * acknowledge these annotations and prevent annotator from annotating such elements with
 * {@code @Nullable} annotations.
 */
public class NonnullStore extends Registry<Location> {

  public NonnullStore(ImmutableSet<ModuleConfiguration> modules, Context context) {
    super(
        modules.stream()
            .map(moduleInfo -> moduleInfo.dir.resolve(Serializer.NON_NULL_ELEMENTS_FILE_NAME))
            .collect(ImmutableSet.toImmutableSet()),
        context);
  }

  @Override
  protected Builder<Location> getBuilder() {
    return Location::createLocationFromArrayInfo;
  }

  /**
   * Returns true if the element at the given location has an explicit {@code @Nonnull} annotation.
   *
   * @param target Location of the given element.
   * @return true, if the element at the given location has an explicit {@code @Nonnull} annotation.
   */
  public boolean hasExplicitNonnullAnnotation(Location target) {
    return findRecordWithHashHint(location -> location.equals(target), target.hashCode()) != null;
  }
}
