/*
 * MIT License
 *
 * Copyright (c) 2023 Nima Karimipour
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

package edu.ucr.cs.riple.core.checkers.ucrtaint;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.checkers.CheckerBaseClass;
import edu.ucr.cs.riple.core.injectors.AnnotationInjector;
import edu.ucr.cs.riple.core.metadata.Context;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.trackers.Region;
import java.util.Set;

/**
 * Represents <a href="https://github.com/kanaksad/UCRTaintingChecker">UCRTaint</a> checker in
 * Annotator.
 */
public class UCRTaint extends CheckerBaseClass<TaintError> {

  /** The name of the checker. This is used to identify the checker in the configuration file. */
  public static final String NAME = "UCRTaint";

  public UCRTaint(Config config) {
    super(config, 0);
  }

  @Override
  public Set<TaintError> deserializeErrors(Context context) {
    return null;
  }

  @Override
  public void suppressRemainingAnnotations(Config config, AnnotationInjector injector) {
    throw new RuntimeException(
        "Suppression for remaining errors is not supported for " + NAME + "yet!");
  }

  @Override
  public void verifyCheckerCompatibility(int version) {
    if (version != 0) {
      throw new RuntimeException(
          "This version of annotator is only compatible with UCR Taint version 0, but found: "
              + version);
    }
  }

  @Override
  public void prepareConfigFilesForBuild(Context context) {
    // TODO: implement this once configuration on UCRTaint is finalized.
  }

  @Override
  public TaintError createErrorFactory(
      String errorType,
      String errorMessage,
      Region region,
      int offset,
      ImmutableSet<Fix> resolvingFixes) {
    return null;
  }
}
