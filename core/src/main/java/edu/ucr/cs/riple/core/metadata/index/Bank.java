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

import com.google.common.base.Preconditions;
import edu.ucr.cs.riple.core.metadata.trackers.Region;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class Bank<T extends Hashable> {

  private final Index<T> rootInClass;
  private final Index<T> rootInMethod;
  private Index<T> currentInMethod;
  private Index<T> currentInClass;
  private final Factory<T> factory;
  private final Path path;

  public Bank(Path path, Factory<T> factory) {
    this.factory = factory;
    this.path = path;
    rootInClass = new Index<>(path, Index.Type.BY_CLASS, factory);
    rootInMethod = new Index<>(path, Index.Type.BY_METHOD, factory);
    rootInMethod.index();
    rootInClass.index();
    Preconditions.checkArgument(rootInClass.total == rootInMethod.total);
  }

  public void saveState(boolean saveClass, boolean saveMethod) {
    if (saveClass) {
      currentInClass = new Index<>(this.path, Index.Type.BY_CLASS, factory);
      currentInClass.index();
    }
    if (saveMethod) {
      currentInMethod = new Index<>(this.path, Index.Type.BY_METHOD, factory);
      currentInMethod.index();
    }
  }

  private Result<T> compareByList(List<T> previousItems, List<T> currentItems) {
    int size = currentItems.size() - previousItems.size();
    previousItems.forEach(currentItems::remove);
    return new Result<>(size, currentItems);
  }

  public Result<T> compareByMethod(String className, String methodName, boolean fresh) {
    saveState(false, fresh);
    return compareByList(
        rootInMethod.getByMethod(className, methodName),
        currentInMethod.getByMethod(className, methodName));
  }

  public Result<T> compare() {
    return compareByList(rootInMethod.getAllEntities(), currentInMethod.getAllEntities());
  }

  public Set<Region> getRegionsForFixes(Predicate<T> c) {
    return rootInClass.getRegionsForFixes(c);
  }
}
