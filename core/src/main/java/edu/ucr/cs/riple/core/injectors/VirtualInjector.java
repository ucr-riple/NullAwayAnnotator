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
import com.google.common.base.Predicate;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.injector.changes.Change;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Wrapper tool used to inject annotations Virtually to the source code. This injector serializes
 * requested changes to a file which later can be read by a library model loaders and make its
 * impact by library models.
 */
public class VirtualInjector extends AnnotationInjector {

  public VirtualInjector(Config config) {
    super(config);
  }

  @Override
  public void injectFixes(Set<Fix> fixes) {
    if (!config.downStreamDependenciesAnalysisActivated) {
      throw new IllegalStateException(
          "Downstream dependencies not activated, cannot inject annotations virtually!");
    }
    // Path to serialize annotations to library model loader path.
    Path path = config.nullawayLibraryModelLoaderPath;
    Preconditions.checkNotNull(
        path,
        "NullawayLibraryModelLoaderPath cannot be null while Downstream dependencies analysis is activated.");
    try (OutputStream os = new FileOutputStream(path.toFile())) {
      Set<String> rows =
          fixes.stream()
              .filter((Predicate<Fix>) Fix::isOnMethod)
              .map(fix -> fix.toMethod().clazz + "\t" + fix.toMethod().method)
              .collect(Collectors.toSet());
      for (String row : rows) {
        os.write(row.getBytes(Charset.defaultCharset()), 0, row.length());
      }
      os.flush();
    } catch (IOException e) {
      throw new RuntimeException("Error happened for writing at file: " + path, e);
    }
  }

  @Override
  public <T extends Change> void applyChanges(Set<T> changes) {
    throw new UnsupportedOperationException(
        "Cannot remove/add annotations with this injector, use physical injector instead.");
  }
}
