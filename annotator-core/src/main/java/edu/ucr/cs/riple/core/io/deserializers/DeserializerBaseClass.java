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

package edu.ucr.cs.riple.core.io.deserializers;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.Checker;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.metadata.Context;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.trackers.Region;
import edu.ucr.cs.riple.injector.location.OnField;
import java.util.Set;

/** Base class for all checker deserializers. */
public abstract class DeserializerBaseClass implements CheckerDeserializer {

  /** Annotator config. */
  protected final Config config;
  /** The checker that this deserializer is supporting. */
  protected final Checker checker;
  /** The supporting serialization version of this deserializer. */
  protected final int version;

  public DeserializerBaseClass(Config config, Checker checker, int version) {
    this.config = config;
    this.checker = checker;
    this.version = version;
  }

  /**
   * Creates an {@link Error} instance.
   *
   * @param errorType Error type.
   * @param errorMessage Error message.
   * @param region Region where the error is reported,
   * @param offset offset of program point in original version where error is reported.
   * @param resolvingFixes Set of fixes that resolve the error.
   * @return The corresponding error.
   */
  protected Error createError(
      String errorType,
      String errorMessage,
      Region region,
      int offset,
      ImmutableSet<Fix> resolvingFixes,
      Context context) {
    ImmutableSet<Fix> fixes =
        resolvingFixes.stream()
            .filter(f -> !context.getNonnullStore().hasExplicitNonnullAnnotation(f.toLocation()))
            .collect(ImmutableSet.toImmutableSet());
    return new Error(errorType, errorMessage, region, offset, fixes);
  }

  /**
   * Extends field variable names to full list to include all variables declared in the same
   * statement.
   *
   * @param onField Location of the field.
   * @return The updated given location.
   */
  protected OnField extendVariableList(OnField onField, Context context) {
    Set<String> variables =
        context
            .getFieldRegistry()
            .getInLineMultipleFieldDeclarationsOnField(onField.clazz, onField.variables);
    onField.variables.addAll(variables);
    return onField;
  }

  @Override
  public int getVersionNumber() {
    return version;
  }

  @Override
  public Checker getAssociatedChecker() {
    return checker;
  }
}
