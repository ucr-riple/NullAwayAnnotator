/*
 * MIT License
 *
 * Copyright (c) 2022 Nima Karimipour
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

package edu.ucr.cs.riple.core.evaluators.suppliers;

import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.injectors.AnnotationInjector;
import edu.ucr.cs.riple.core.metadata.Context;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.ErrorStore;

/** Base class for all instances of {@link Supplier}. */
public abstract class AbstractSupplier implements Supplier {

  /** Error Store instance. */
  protected final ErrorStore errorStore;
  /** Injector instance. */
  protected final AnnotationInjector injector;
  /** Context of the module which the impact of fixes are computed on. */
  protected final Context context;
  /** Depth of analysis. */
  protected final int depth;
  /** Annotator config. */
  protected final Config config;

  public AbstractSupplier(Config config, Context context) {
    this.config = config;
    this.context = context;
    this.errorStore = new ErrorStore(config, context);
    this.injector = initializeInjector();
    this.depth = initializeDepth();
  }

  /**
   * Initializer for injector.
   *
   * @return {@link AnnotationInjector} instance.
   */
  protected abstract AnnotationInjector initializeInjector();

  /**
   * Initializer for depth.
   *
   * @return depth of analysis.
   */
  protected abstract int initializeDepth();

  /**
   * Initializer for error store.
   *
   * @return {@link ErrorStore} of {@link Error} instances.
   */
  @Override
  public ErrorStore getErrorStore() {
    return errorStore;
  }

  @Override
  public AnnotationInjector getInjector() {
    return injector;
  }

  @Override
  public int depth() {
    return depth;
  }

  @Override
  public Config getConfig() {
    return config;
  }
}
