/*
 * MIT License
 *
 * Copyright (c) 2020 Nima Karimipour
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

import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.code.Symbol;
import edu.ucr.cs.riple.scanner.Serializer;
import java.nio.file.Path;
import javax.annotation.Nullable;

/** Container for storing class flat name and url to source file containing class. */
public class ClassRecord {
  /** Containing class symbol. */
  public final Symbol.ClassSymbol clazz;

  /** Path to url containing this class. */
  @Nullable public final Path path;

  public ClassRecord(Symbol.ClassSymbol clazz, CompilationUnitTree compilationUnitTree) {
    this.clazz = clazz;
    this.path = Serializer.pathToSourceFileFromURI(compilationUnitTree.getSourceFile().toUri());
  }

  public static String header() {
    return "class" + '\t' + "path";
  }

  @Override
  public String toString() {
    return clazz.flatName() + "\t" + ((path == null) ? "null" : path);
  }
}
