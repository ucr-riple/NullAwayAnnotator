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

import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.Context;
import edu.ucr.cs.riple.core.checkers.nullaway.NullAway;
import edu.ucr.cs.riple.core.module.ModuleInfo;
import edu.ucr.cs.riple.core.registries.index.Error;
import edu.ucr.cs.riple.injector.location.OnField;
import java.util.Set;

/** Base class for all checker representations. */
public abstract class CheckerBaseClass<T extends Error> implements Checker<T> {

  /** Annotator config. */
  protected final Config config;
  /** Annotator context. */
  protected final Context context;

  public CheckerBaseClass(Context context) {
    this.context = context;
    this.config = context.config;
  }

  /**
   * Extends field variable names to full list to include all variables declared in the same
   * statement.
   *
   * @param onField Location of the field.
   * @return The updated given location.
   */
  protected static OnField extendVariableList(OnField onField, ModuleInfo moduleInfo) {
    Set<String> variables =
        moduleInfo
            .getFieldRegistry()
            .getInLineMultipleFieldDeclarationsOnField(onField.clazz, onField.variables);
    onField.variables.addAll(variables);
    return onField;
  }

  /**
   * Returns the checker instance by its name.
   *
   * @param name name of the checker.
   * @param context annotator context.
   * @return the checker instance with the given name.
   */
  public static Checker<?> getCheckerByName(String name, Context context) {
    if (name == null) {
      throw new RuntimeException("Checker name is null");
    }
    switch (name) {
      case NullAway.NAME:
        return new NullAway(context);
      default:
        throw new RuntimeException("Unknown checker name: " + name);
    }
  }
}
