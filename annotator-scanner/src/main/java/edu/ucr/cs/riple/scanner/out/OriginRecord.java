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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.tools.javac.code.Symbol;
import edu.ucr.cs.riple.scanner.Serializer;
import edu.ucr.cs.riple.scanner.location.SymbolLocation;
import java.util.Objects;
import java.util.Set;

public class OriginRecord {

  /** Enclosing method of the invoked method. */
  private final Symbol.MethodSymbol encMethod;

  /** The method that is invoked. */
  private final Symbol.MethodSymbol calledMethod;

  /** The in the invoked method that origins are computed for. */
  private final Symbol.VarSymbol parameter;

  /** The set of origins of the parameter. */
  private final Set<Symbol> origins;

  private final int index;

  public OriginRecord(
      Symbol.MethodSymbol encMethod,
      Symbol.MethodSymbol calledMethod,
      int index,
      Set<Symbol> origins) {
    this.encMethod = encMethod;
    this.calledMethod = calledMethod;
    this.index = index;
    this.parameter = calledMethod.getParameters().get(index);
    this.origins = origins;
  }

  /**
   * Returns the header of the record. This is a json file and it does not have a header.
   *
   * @return An empty string.
   */
  public static String header() {
    return "";
  }

  /**
   * Returns the json representation of the record.
   *
   * @return the json representation of the record.
   */
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.addProperty("class", Serializer.serializeSymbol(encMethod.enclClass()));
    json.addProperty("method", Serializer.serializeSymbol(encMethod));
    json.addProperty("calledMethod", Serializer.serializeSymbol(calledMethod));
    json.addProperty("parameter", Serializer.serializeSymbol(parameter));
    json.addProperty("index", index);
    JsonArray originsArray = new JsonArray();
    for (Symbol origin : origins) {
      originsArray.add(SymbolLocation.createLocationFromSymbol(origin).tabSeparatedToString());
    }
    json.add("origins", originsArray);
    return json;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof OriginRecord)) {
      return false;
    }
    OriginRecord that = (OriginRecord) o;
    return Objects.equals(encMethod, that.encMethod)
        && Objects.equals(calledMethod, that.calledMethod)
        && Objects.equals(parameter, that.parameter);
  }

  @Override
  public int hashCode() {
    return Objects.hash(encMethod, calledMethod, parameter);
  }
}
