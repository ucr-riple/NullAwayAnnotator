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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/** A visitor that converts a location to a JSON object. */
public class LocationToJsonVisitor implements LocationVisitor<JSONObject, Void> {

  @SuppressWarnings("unchecked")
  private JSONObject defaultAction(Location location) {
    JSONObject res = new JSONObject();
    res.put(Location.KEYS.CLASS, location.clazz);
    res.put(Location.KEYS.KIND, location.type.toString());
    res.put(Location.KEYS.PATH, location.path);
    return res;
  }

  @SuppressWarnings("unchecked")
  @Override
  public JSONObject visitMethod(OnMethod onMethod, Void unused) {
    JSONObject res = defaultAction(onMethod);
    res.put(Location.KEYS.METHOD, onMethod.method);
    return res;
  }

  @SuppressWarnings("unchecked")
  @Override
  public JSONObject visitField(OnField onField, Void unused) {
    JSONObject res = defaultAction(onField);
    JSONArray fields = new JSONArray();
    fields.addAll(onField.variables);
    res.put(Location.KEYS.VARIABLES, fields);
    return res;
  }

  @SuppressWarnings("unchecked")
  @Override
  public JSONObject visitParameter(OnParameter onParameter, Void unused) {
    JSONObject res = defaultAction(onParameter);
    res.put(Location.KEYS.METHOD, onParameter.method);
    res.put(Location.KEYS.INDEX, onParameter.index);
    return res;
  }

  @Override
  public JSONObject visitClass(OnClass onClass, Void unused) {
    return defaultAction(onClass);
  }
}
