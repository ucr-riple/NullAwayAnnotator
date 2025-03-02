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

package edu.ucr.cs.riple.core.registries.method;

import com.github.javaparser.utils.Pair;
import com.google.common.collect.ImmutableMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import edu.ucr.cs.riple.annotator.util.parsers.JsonParser;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.location.OnMethod;
import java.nio.file.Path;

public class EffectiveMethodRegistry {

  private final ImmutableMultimap<OnMethod, Pair<String, Integer>> effectiveMethods;

  public EffectiveMethodRegistry(Path path) {
    ImmutableMultimap.Builder<OnMethod, Pair<String, Integer>> builder =
        ImmutableMultimap.builder();

    String content = Utility.readFile(path);
    content = content.replace("}{", "},{");
    content = "[" + content + "]";
    content = "{\"effectiveMethods\":" + content + "}";

    JsonObject json = JsonParser.parseJson(content);
    JsonArray effectiveMethods = json.getAsJsonArray("effectiveMethods");
    for (int i = 0; i < effectiveMethods.size(); i++) {
      JsonObject effectiveMethod = effectiveMethods.get(i).getAsJsonObject();
    }

    this.effectiveMethods = builder.build();
  }
}
