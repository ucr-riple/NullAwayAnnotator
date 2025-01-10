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

package edu.ucr.cs.riple.injector.location;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

/** A visitor that converts a location to a JSON object. */
public class LocationToJsonVisitor implements LocationVisitor<JsonObject, Void> {

  /** The Keys used to represent a location in JSON format */
  public enum KEYS {
    VARIABLES,
    METHOD,
    KIND,
    CLASS,
    PATH,
    INDEX,
    SUPER,
  }

  private JsonObject defaultAction(Location location) {
    JsonObject res = new JsonObject();
    res.addProperty(KEYS.CLASS.name(), location.clazz);
    res.addProperty(KEYS.KIND.name(), location.getKind().toString());
    res.addProperty(KEYS.PATH.name(), location.path.toString());
    return res;
  }

  @Override
  public JsonObject visitMethod(OnMethod onMethod, Void unused) {
    JsonObject res = defaultAction(onMethod);
    res.addProperty(KEYS.METHOD.name(), onMethod.method);
    return res;
  }

  @Override
  public JsonObject visitField(OnField onField, Void unused) {
    JsonObject res = defaultAction(onField);
    JsonArray fields = new JsonArray();
    onField.variables.forEach(fields::add);
    res.add(KEYS.VARIABLES.name(), fields);
    return res;
  }

  @Override
  public JsonObject visitParameter(OnParameter onParameter, Void unused) {
    JsonObject res = defaultAction(onParameter);
    res.addProperty(KEYS.METHOD.name(), onParameter.enclosingMethod.method);
    res.addProperty(KEYS.INDEX.name(), onParameter.index);
    return res;
  }

  @Override
  public JsonObject visitClass(OnClass onClass, Void unused) {
    return defaultAction(onClass);
  }

  @Override
  public JsonObject visitLocalVariable(OnLocalVariable onLocalVariable, Void unused) {
    JsonObject res = defaultAction(onLocalVariable);
    res.addProperty(KEYS.METHOD.name(), onLocalVariable.encMethod == null ? "" : onLocalVariable.encMethod.method);
    res.addProperty(KEYS.VARIABLES.name(), onLocalVariable.varName);
    return res;
  }

  @Override
  public JsonObject visitClassDeclaration(OnClassDeclaration onClassDeclaration, Void unused) {
    JsonObject res = defaultAction(onClassDeclaration);
    res.addProperty(KEYS.SUPER.name(), onClassDeclaration.target);
    return res;
  }
}
