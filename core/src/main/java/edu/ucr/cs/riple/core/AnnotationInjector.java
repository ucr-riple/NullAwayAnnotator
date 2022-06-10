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

package edu.ucr.cs.riple.core;

import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.injector.Change;
import edu.ucr.cs.riple.injector.Injector;
import edu.ucr.cs.riple.injector.WorkListBuilder;
import java.util.Set;
import java.util.stream.Collectors;

public class AnnotationInjector {
  private final Injector injector;

  public AnnotationInjector(Config config) {
    this.injector = Injector.builder().keepStyle(!config.lexicalPreservationDisabled).build();
  }

  public void removeFixes(Set<Fix> fixes) {
    if (fixes == null || fixes.size() == 0) {
      return;
    }
    Set<Change> toRemove =
        fixes.stream()
            .map(fix -> new Change(fix.change.location, fix.annotation, false))
            .collect(Collectors.toSet());
    injector.start(new WorkListBuilder(toRemove).getWorkLists(), false);
  }

  public void injectFixes(Set<Fix> fixes) {
    if (fixes == null || fixes.size() == 0) {
      return;
    }
    injectChanges(fixes.stream().map(fix -> fix.change).collect(Collectors.toSet()));
  }

  public void injectChanges(Set<Change> changes) {
    if (changes == null || changes.size() == 0) {
      return;
    }
    injector.start(new WorkListBuilder(changes).getWorkLists(), false);
  }
}
