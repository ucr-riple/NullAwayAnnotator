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

package com.example.tool.core.injectors;

import com.example.tool.core.Config;
import com.example.tool.injector.Injector;
import com.example.tool.injector.changes.AddAnnotation;
import com.example.tool.injector.changes.RemoveAnnotation;
import com.example.tool.injector.offsets.FileOffsetStore;
import java.util.Set;

/** Wrapper tool used to inject annotations Physically to the source code. */
public class PhysicalInjector extends AnnotationInjector {
  private final Injector injector;

  public PhysicalInjector(Config config) {
    super(config);
    this.injector = new Injector();
  }

  @Override
  public void removeAnnotations(Set<RemoveAnnotation> changes) {
    Set<FileOffsetStore> offsetStores = injector.removeAnnotations(changes);
    config.offsetHandler.updateStateWithRecentChanges(offsetStores);
  }

  @Override
  public void injectAnnotations(Set<AddAnnotation> changes) {
    Set<FileOffsetStore> offsetStores = injector.addAnnotations(changes);
    config.offsetHandler.updateStateWithRecentChanges(offsetStores);
  }
}
