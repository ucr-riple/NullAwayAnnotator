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

package edu.ucr.cs.riple.core.injectors;

import edu.ucr.cs.riple.core.Context;
import edu.ucr.cs.riple.core.registries.index.Fix;
import edu.ucr.cs.riple.injector.changes.AddAnnotation;
import edu.ucr.cs.riple.injector.changes.RemoveAnnotation;
import java.util.Set;
import java.util.stream.Collectors;

/** Wrapper tool used to inject annotations to the source code. */
public abstract class AnnotationInjector {
  /** Core context. */
  protected final Context context;

  public AnnotationInjector(Context context) {
    this.context = context;
  }

  /**
   * Removes fixes from source code.
   *
   * @param fixes List of fixes to remove.
   */
  public void removeFixes(Set<Fix> fixes) {
    if (fixes == null || fixes.size() == 0) {
      return;
    }
    Set<RemoveAnnotation> toRemove =
        fixes.stream()
            .flatMap(fix -> fix.changes.stream().map(AddAnnotation::getReverse))
            .collect(Collectors.toSet());
    removeAnnotations(toRemove);
  }

  /**
   * Applies fixes to the source code.
   *
   * @param fixes Set of fixes to apply.
   */
  public void injectFixes(Set<Fix> fixes) {
    if (fixes == null || fixes.size() == 0) {
      return;
    }
    injectAnnotations(
        fixes.stream().flatMap(fix -> fix.changes.stream()).collect(Collectors.toSet()));
  }

  /**
   * Removes annotation from the source code.
   *
   * @param changes Set of annotations to remove.
   */
  public abstract void removeAnnotations(Set<RemoveAnnotation> changes);

  /**
   * Injects annotations to the source code.
   *
   * @param changes Set of annotations to inject.
   */
  public abstract void injectAnnotations(Set<AddAnnotation> changes);

  /**
   * Removes annotation from the source code.
   *
   * @param change Annotation to inject.
   */
  public void removeAnnotation(RemoveAnnotation change) {
    this.removeAnnotations(Set.of(change));
  }

  /**
   * Injects annotations to the source code.
   *
   * @param change Annotation to inject.
   */
  public void injectAnnotation(AddAnnotation change) {
    this.injectAnnotations(Set.of(change));
  }
}
