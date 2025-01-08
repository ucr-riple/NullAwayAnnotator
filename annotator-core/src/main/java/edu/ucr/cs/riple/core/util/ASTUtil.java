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

package edu.ucr.cs.riple.core.util;

import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.registries.region.Region;
import edu.ucr.cs.riple.injector.Injector;
import java.nio.file.Path;

public class ASTUtil {

  public static boolean isObjectEqualsMethod(String member) {
    if (!member.contains("(")) {
      return false;
    }
    String methodName = member.substring(0, member.indexOf("("));
    // check if it has only one parameter and the parameter is an java.lang.Object
    String parameter = member.substring(member.indexOf("(") + 1, member.indexOf(")"));
    if (!parameter.equals("java.lang.Object")) {
      return false;
    }
    return methodName.equals("equals");
  }

  public static String getRegionSourceCode(Config config, Path path, Region region) {
    return new Injector(config.languageLevel)
        .getMethodSourceCode(path, region.clazz, region.member);
  }
}
