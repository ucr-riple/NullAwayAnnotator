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

package edu.ucr.cs.riple.core.injectors;

import com.google.common.base.Preconditions;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.injector.changes.AddAnnotation;
import edu.ucr.cs.riple.injector.changes.RemoveAnnotation;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Wrapper tool used to inject annotations virtually to the source code. This injector serializes
 * requested changes to a file which later can be read by a library model loaders and make its
 * impact by library models.
 */
public class VirtualInjector extends AnnotationInjector {

  /** Path to library model loader */
  private final Path libraryModelPath;

  public VirtualInjector(Config config) {
    super(config);
    this.libraryModelPath = config.nullawayLibraryModelLoaderPath;
    if (config.downStreamDependenciesAnalysisActivated) {
      Preconditions.checkNotNull(
          libraryModelPath,
          "NullawayLibraryModelLoaderPath cannot be null while downstream dependencies analysis is activated.");
      clear();
    }
  }

  @Override
  public void removeAnnotations(Set<RemoveAnnotation> changes) {
    clear();
  }

  @Override
  public void injectAnnotations(Set<AddAnnotation> changes) {
    if (!config.downStreamDependenciesAnalysisActivated) {
      throw new IllegalStateException(
          "Downstream dependencies analysis not activated, cannot inject annotations virtually!");
    }
    try (BufferedOutputStream os =
        new BufferedOutputStream(new FileOutputStream(libraryModelPath.toFile()))) {
      Set<String> rows =
          changes.stream()
              .filter(addAnnotation -> addAnnotation.getLocation().isOnMethod())
              .map(
                  annot ->
                      annot.getLocation().clazz
                          + "\t"
                          + annot.getLocation().toMethod().method
                          + "\n")
              .collect(Collectors.toSet());
      for (String row : rows) {
        os.write(row.getBytes(Charset.defaultCharset()), 0, row.length());
      }
      os.flush();
    } catch (IOException e) {
      throw new RuntimeException("Error happened for writing at file: " + libraryModelPath, e);
    }
  }

  /** Removes any existing entry from library models. */
  private void clear() {
    try {
      new FileOutputStream(libraryModelPath.toFile()).close();
    } catch (IOException e) {
      throw new RuntimeException("Could not clear library model loader content", e);
    }
  }
}
