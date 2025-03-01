/*
 * MIT License
 *
 * Copyright (c) 2025 Nima Karimipour
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

package edu.ucr.cs.riple.scanner.out;

import com.sun.tools.javac.code.Symbol;
import edu.ucr.cs.riple.scanner.Serializer;
import java.util.Objects;

public class EffectiveMethodRecord {

  /** Method that uses the parameter in an effective way. */
  private final Symbol.VarSymbol parameter;

  /** Symbol of the method. */
  private final Symbol.MethodSymbol methodSymbol;

  private final int index;

  public EffectiveMethodRecord(Symbol.MethodSymbol methodSymbol, Symbol.VarSymbol parameter) {
    this.parameter = parameter;
    this.methodSymbol = methodSymbol;
    this.index = methodSymbol.getParameters().indexOf(parameter);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof EffectiveMethodRecord)) {
      return false;
    }
    EffectiveMethodRecord that = (EffectiveMethodRecord) o;
    return Objects.equals(parameter, that.parameter)
        && Objects.equals(methodSymbol, that.methodSymbol);
  }

  @Override
  public int hashCode() {
    return Objects.hash(parameter, methodSymbol);
  }

  @Override
  public String toString() {
    return Serializer.serializeSymbol(methodSymbol.enclClass())
        + "\t"
        + Serializer.serializeSymbol(methodSymbol)
        + "\t"
        + Serializer.serializeSymbol(parameter)
        + "\t"
        + index;
  }

  public static String header() {
    return "CLASS\tMETHOD\tPARAMETER\tINDEX";
  }
}
