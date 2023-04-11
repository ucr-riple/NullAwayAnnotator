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

package edu.ucr.cs.riple.core.checkers;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.injectors.AnnotationInjector;
import edu.ucr.cs.riple.core.metadata.Context;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.trackers.Region;
import java.util.Set;

/**
 * Represents a checker that is running on the target module.
 *
 * @param <T> Type of errors reported by the checker.
 */
public interface Checker<T extends Error> {

  /**
   * Deserializes errors reported by the checker from the output using the given context.
   *
   * @param context Context of the modules which errors are reported on.
   * @return Set of errors reported by the checker.
   */
  Set<T> deserializeErrors(Context context);

  /**
   * Suppresses remaining errors reported by the checker.
   *
   * @param config Annotator config.
   * @param injector Annotation injector to inject selected annotations.
   */
  void suppressRemainingAnnotations(Config config, AnnotationInjector injector);

  /**
   * Creates an {@link Error} instance from the given parameters.
   *
   * @param errorType Error type.
   * @param errorMessage Error message.
   * @param region Region where the error is reported,
   * @param offset offset of program point in original version where error is reported.
   * @param resolvingFixes Set of fixes that resolve the error.
   * @return The corresponding error.
   */
  T createErrorFactory(
      String errorType,
      String errorMessage,
      Region region,
      int offset,
      ImmutableSet<Fix> resolvingFixes);

  /**
   * Verifies that the checker representation in Annotator is compatible with the actual running
   * checker on the target module.
   *
   * @param version The version of the actual running checker.
   */
  void verifyCheckerCompatibility(int version);

  void prepareConfigFilesForBuild(Context context);
}
