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

package edu.ucr.cs.riple.scanner.generatedcode;

import com.google.common.collect.ImmutableSet;
import com.sun.source.util.TreePath;
import java.util.stream.Collectors;

/**
 * Responsible for resolving the corresponding {@link SourceType} for an element at the given {@link
 * TreePath}.
 */
public class SymbolSourceResolver {

  /** Set of activated code generator detectors. */
  private final ImmutableSet<GeneratedCodeDetector> generatedCodeDetectors;

  public SymbolSourceResolver(ImmutableSet<SourceType> activatedSourceDetectors) {
    if (activatedSourceDetectors.size() == 0) {
      this.generatedCodeDetectors = ImmutableSet.of();
      return;
    }
    if (activatedSourceDetectors.size() == 1
        && activatedSourceDetectors.iterator().next().equals(SourceType.LOMBOK)) {
      this.generatedCodeDetectors = ImmutableSet.of(new LombokGeneratedCodeDetector());
      return;
    }
    // Useful when annotator-core is updated but still using an old version of annotator-scanner,
    // this exception can inform the user.
    throw new RuntimeException(
        "Unrecognized resource detectors are requested: "
            + activatedSourceDetectors.stream().map(Enum::name).collect(Collectors.toSet()));
  }

  /**
   * Returns source for the element at the given path. If the element exists in the source code
   * {@code "SOURCE"} will be returned and otherwise the name of the code generator will be
   * returned.
   *
   * @return Associated Source type of the generator that produced the code. If element exists in
   *     source code and not produced by any processor, it will return {@link SourceType#SOURCE}
   */
  public SourceType getSourceForSymbolAtPath(TreePath path) {
    for (GeneratedCodeDetector detector : this.generatedCodeDetectors) {
      if (detector.isGeneratedCode(path)) {
        return detector.getGeneratorSourceType();
      }
    }
    return SourceType.SOURCE;
  }
}
