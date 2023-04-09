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

package edu.ucr.cs.riple.injector.visitors;

import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.OnClass;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.injector.location.OnLocalVariable;
import edu.ucr.cs.riple.injector.location.OnMethod;
import edu.ucr.cs.riple.injector.location.OnParameter;

/**
 * A visitor of types, in the style of the visitor design pattern. When a visitor is passed to a
 * location's {@link Location#accept accept} method, the <code>visit<i>Xyz</i></code> method
 * applicable to that location is invoked.
 */
public interface LocationVisitor<R, P> {

  /**
   * Visits a location for a method.
   *
   * @param onMethod the location for a method
   * @param p a visitor-specified parameter
   * @return a visitor-specified result
   */
  R visitMethod(OnMethod onMethod, P p);

  /**
   * Visits a location for a field.
   *
   * @param onField the location for a field
   * @param p a visitor-specified parameter
   * @return a visitor-specified result
   */
  R visitField(OnField onField, P p);

  /**
   * Visits a location for a parameter.
   *
   * @param onParameter the location for a parameter
   * @param p a visitor-specified parameter
   * @return a visitor-specified result
   */
  R visitParameter(OnParameter onParameter, P p);

  /**
   * Visits a location for a class.
   *
   * @param onClass the location for a class
   * @param p a visitor-specified parameter
   * @return a visitor-specified result
   */
  R visitClass(OnClass onClass, P p);

  /**
   * Visits a location for a local variable.
   *
   * @param onLocalVariable the location for a local variable
   * @param p a visitor-specified parameter
   * @return a visitor-specified result
   */
  R visitLocalVariable(OnLocalVariable onLocalVariable, P p);
}
