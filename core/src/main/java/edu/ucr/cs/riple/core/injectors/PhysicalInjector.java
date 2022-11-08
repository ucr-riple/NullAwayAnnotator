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

import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.injector.Injector;
import edu.ucr.cs.riple.injector.changes.AddAnnotation;
import edu.ucr.cs.riple.injector.changes.RemoveAnnotation;
import java.util.Set;

/** Wrapper tool used to inject annotations Physically to the source code. */
public class PhysicalInjector extends AnnotationInjector {
  private final Injector injector;

  public PhysicalInjector(Config config) {
    super(config);
    this.injector =
        Injector.builder().preserveStyle(!this.config.lexicalPreservationDisabled).build();
  }

  @Override
  public void removeAnnotations(Set<RemoveAnnotation> changes) {
    this.injector.addAnnotations(changes);
  }

  @Override
  public void injectAnnotations(Set<AddAnnotation> changes) {
    this.injector.removeAnnotations(changes);
  }
}
