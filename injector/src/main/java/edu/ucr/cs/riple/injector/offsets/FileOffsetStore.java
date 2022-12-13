/*
 * MIT License
 *
 * Copyright (c) 2022 Nima Karimipour
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

package edu.ucr.cs.riple.injector.offsets;

import static java.util.Comparator.comparingInt;

import java.nio.file.Path;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Stores list of offset changes for a file. */
public class FileOffsetStore {

  /** Path to file. */
  private final Path path;
  /** List of existing offset changes. */
  private final SortedSet<OffsetChange> offsetChanges;
  /** Contents of file. */
  private final List<String> lines;

  public FileOffsetStore(List<String> lines, Path path) {
    this.lines = lines;
    this.path = path;
    this.offsetChanges = new TreeSet<>();
  }

  /**
   * Adds an offset change for addition.
   *
   * @param line Line number where addition occurred.
   * @param column Column number where addition occurred.
   * @param numChars Number of characters added.
   */
  public void updateOffsetWithAddition(int line, int column, int numChars) {
    int offset = characterOffsetAtLine(line);
    this.offsetChanges.add(new OffsetChange(offset + column, numChars));
  }

  /**
   * Adds an offset change for addition along a new line addition.
   *
   * @param line Line number where addition occurred.
   * @param numChars Number of characters added.
   */
  public void updateOffsetWithNewLineAddition(int line, int numChars) {
    int offset = characterOffsetAtLine(line);
    // add one to numChars for new line.
    this.offsetChanges.add(new OffsetChange(offset, numChars + 1));
  }

  /**
   * Adds an offset change for deletion.
   *
   * @param line Line number where deletion occurred.
   * @param column Column number where deletion occurred.
   * @param numChars Number of characters removed.
   */
  public void updateOffsetWithDeletion(int line, int column, int numChars) {
    int offset = characterOffsetAtLine(line);
    this.offsetChanges.add(new OffsetChange(offset + column, -1 * numChars));
  }

  /**
   * Returns number of characters before a line.
   *
   * @param line line number.
   * @return Number of characters before reaching a line.
   */
  private int characterOffsetAtLine(int line) {
    int ans = 0;
    for (int current = 0; current < line && current < lines.size(); current++) {
      ans += lines.get(current).length();
      ans += 1; // for new line.
    }
    return ans;
  }

  /**
   * Getter for path.
   *
   * @return Path of target file.
   */
  public Path getPath() {
    return path;
  }

  /**
   * Returns offset changes translated to original offsets according to existing offset changes.
   *
   * @param existingOffsetChanges List of existing offset changes.
   * @return Translated and sorted offsets.
   */
  public SortedSet<OffsetChange> getOffsetWithoutChanges(
      SortedSet<OffsetChange> existingOffsetChanges) {
    return offsetChanges.stream()
        .map(offsetChange -> offsetChange.getOffsetWithoutChanges(existingOffsetChanges))
        .sorted(comparingInt(o -> o.position))
        .collect(Collectors.toCollection(TreeSet::new));
  }
}
