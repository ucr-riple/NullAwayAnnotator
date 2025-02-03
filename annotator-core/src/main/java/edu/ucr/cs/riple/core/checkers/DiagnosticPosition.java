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

package edu.ucr.cs.riple.core.checkers;

import edu.ucr.cs.riple.core.util.Utility;
import java.nio.file.Path;
import java.util.List;

/** Represents a position in a file where a diagnostic is reported. */
public class DiagnosticPosition {

  /** The offset in the file where the diagnostic is reported. */
  public final int offset;

  /** The line number where the diagnostic is reported. */
  public final int lineNumber;

  /** The line of source code associated with the diagnostic offset. */
  public final String diagnosticLine;

  /** This offset is adapted to offset in the program that no operation by Annotator is applied. */
  public final int adaptedOffset;

  /** The offset in the line where the diagnostic is reported. */
  public final int offsetInLine;

  public DiagnosticPosition(Path path, int offset, int adaptedOffset) {
    List<String> content = Utility.readFileLines(path);
    int index = 0;
    int lineNum;
    for (lineNum = 0; lineNum < content.size(); lineNum++) {
      String line = content.get(lineNum);
      if (index + line.length() >= offset) {
        break;
      }
      index += line.length() + 1;
    }
    this.adaptedOffset = adaptedOffset;
    this.offset = offset;
    this.offsetInLine = offset - index;
    this.lineNumber = lineNum;
    this.diagnosticLine = content.get(lineNumber);
  }

  public DiagnosticPosition() {
    this.offset = 0;
    this.lineNumber = 0;
    this.diagnosticLine = "";
    this.adaptedOffset = 0;
    this.offsetInLine = 0;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof DiagnosticPosition)) {
      return false;
    }
    DiagnosticPosition position = (DiagnosticPosition) o;
    return adaptedOffset == position.adaptedOffset;
  }

  @Override
  public int hashCode() {
    return adaptedOffset;
  }

  @Override
  public String toString() {
    return adaptedOffset + "";
  }
}
