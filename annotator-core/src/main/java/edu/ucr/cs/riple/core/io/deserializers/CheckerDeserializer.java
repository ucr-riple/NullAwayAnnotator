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

import edu.ucr.cs.riple.core.Checker;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.module.ModuleInfo;
import java.util.Set;

/**
 * Responsible for performing tasks related to NullAway / Type Annotator Scanner serialization
 * features.
 */
public interface CheckerDeserializer {

  /**
   * Deserialized errors reported by the checker on the passed modules.
   *
   * @param moduleInfo ModuleInfo of the module where errors are reported.
   * @return Corresponding Error instance with the passed values.
   */
  Set<Error> deserializeErrors(ModuleInfo moduleInfo);

  /**
   * Returns the serialization version number which this adapter is associated with.
   *
   * @return Serialization number.
   */
  int getVersionNumber();

  /**
   * Returns the name of the checker that this deserializer is associated with.
   *
   * @return Associated checker.
   */
  Checker getAssociatedChecker();
}
