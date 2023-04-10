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

package edu.ucr.cs.riple.core;

import edu.ucr.cs.riple.core.checkers.CheckerDeserializer;
import edu.ucr.cs.riple.core.checkers.deserializers.nullaway.NullAwayV3Deserializer;
import javax.annotation.Nullable;

/** Enum class to represent the supported checkers. */
public enum Checker {
  NULLAWAY() {
    @Override
    public CheckerDeserializer getDeserializer(Config config) {
      return new NullAwayV3Deserializer(config);
    }
  };

  /**
   * Returns the deserializer for the checker.
   *
   * @param config Annotator config.
   * @return The corresponding deserializer.
   */
  public abstract CheckerDeserializer getDeserializer(Config config);

  /**
   * Returns the checker enum value corresponding to the given checker name.
   *
   * @param checkerName Name of the checker.
   * @return Corresponding checker enum value.
   */
  public static Checker getCheckerByName(@Nullable String checkerName) {
    if (checkerName == null) {
      throw new RuntimeException("Checker name cannot be null");
    }
    for (Checker checker : Checker.values()) {
      if (checker.name().equalsIgnoreCase(checkerName)) {
        return checker;
      }
    }
    throw new RuntimeException("No checker found for name: " + checkerName);
  }
}
