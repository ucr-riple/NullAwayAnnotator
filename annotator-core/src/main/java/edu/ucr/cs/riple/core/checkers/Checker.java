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
import edu.ucr.cs.riple.core.injectors.AnnotationInjector;
import edu.ucr.cs.riple.core.module.ModuleConfiguration;
import edu.ucr.cs.riple.core.module.ModuleInfo;
import edu.ucr.cs.riple.core.registries.index.Error;
import edu.ucr.cs.riple.core.registries.index.Fix;
import edu.ucr.cs.riple.core.registries.region.Region;
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
   * @param module Module where the checker reports errors.
   * @return Set of errors reported by the checker.
   */
  Set<T> deserializeErrors(ModuleInfo module);

  /**
   * Suppresses remaining errors reported by the checker.
   *
   * @param injector Annotation injector to inject selected annotations.
   */
  void suppressRemainingErrors(AnnotationInjector injector);

  /**
   * Used to do any pre-processing steps before running the inference.
   *
   * @param injector Annotation injector, can be used to inject any annotations during the
   *     pre-processing phase.
   */
  void preprocess(AnnotationInjector injector);

  /**
   * Verifies that the checker representation in Annotator is compatible with the actual running
   * checker on the target module.
   */
  void verifyCheckerCompatibility();

  /**
   * Prepares the config files for the checker to run on the target module.
   *
   * @param configurations Module configurations where their config files should be prepared for a
   *     build.
   */
  void prepareConfigFilesForBuild(ImmutableSet<ModuleConfiguration> configurations);
}
