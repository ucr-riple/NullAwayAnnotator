package edu.ucr.cs.riple.injector;

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

import com.github.javaparser.Range;
import edu.ucr.cs.riple.injector.util.ASTUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Represents the actual source code in the given range. */
public class SourceCode {
  /** Range of the source code. */
  public final com.github.javaparser.Range range;

  /** Content of the source code. */
  public final String content;

  /**
   * Creates a new source code object.
   *
   * @param path Path to the file.
   * @param range Range of the source code.
   */
  public SourceCode(Path path, Range range) {
    this.range = range;
    String content;
    try {
      content = Files.readString(path);
      this.content =
          content
                  .substring(
                      ASTUtils.computeIndexFromPosition(content, range.begin),
                      ASTUtils.computeIndexFromPosition(content, range.end))
                  .trim()
              // The end position is exclusive, so we need to add 1 to include the last
              // character which is the enclosing brace.
              + "\n}";
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
