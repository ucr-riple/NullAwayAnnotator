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

import edu.ucr.cs.riple.injector.Helper;
import java.nio.file.Path;
import org.json.simple.JSONObject;

public class OnClassDeclaration extends Location {

  public final String target;

  public OnClassDeclaration(Path path, String clazz, String target) {
    super(LocationKind.CLASS_DECL, path, clazz);
    this.target = target;
  }

  public OnClassDeclaration(String path, String clazz, String target) {
    super(LocationKind.CLASS_DECL, Helper.deserializePath(path), clazz);
    this.target = target;
  }

  public OnClassDeclaration(JSONObject json) {
    super(LocationKind.CLASS_DECL, json);
    this.target = (String) json.get("target");
  }

  @Override
  public <R, P> R accept(LocationVisitor<R, P> v, P p) {
    return v.visitClassDeclaration(this, p);
  }
}
