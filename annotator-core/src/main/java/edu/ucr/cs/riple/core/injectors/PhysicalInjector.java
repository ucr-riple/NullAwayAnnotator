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
import edu.ucr.cs.riple.injector.Injector;
import edu.ucr.cs.riple.injector.changes.AddAnnotation;
import edu.ucr.cs.riple.injector.changes.RemoveAnnotation;
import edu.ucr.cs.riple.injector.offsets.FileOffsetStore;
import java.util.Set;

/** Wrapper tool used to inject annotations Physically to the source code. */
public class PhysicalInjector extends AnnotationInjector {
  private final Injector injector;

  /**
   * Creates a new PhysicalInjector instance.
   *
   * @param context Annotator context, required to keep record of the changes made to the source
   *     code to compute the original offsets of reported errors. Original offset of an error, is
   *     the offset of the error in the source code before any changes are made to the source code.
   */
  public PhysicalInjector(Context context) {
    super(context);
    this.injector = new Injector(context.config.languageLevel);
  }

  @Override
  public void removeAnnotations(Set<RemoveAnnotation> changes) {
    Set<FileOffsetStore> offsetStores = injector.removeAnnotations(changes);
    context.offsetHandler.updateStateWithRecentChanges(offsetStores);
  }

  @Override
  public void injectAnnotations(Set<AddAnnotation> changes) {
    Set<FileOffsetStore> offsetStores = injector.addAnnotations(changes);
    context.offsetHandler.updateStateWithRecentChanges(offsetStores);
  }
}
