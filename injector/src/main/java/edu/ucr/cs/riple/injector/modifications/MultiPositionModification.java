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

package edu.ucr.cs.riple.injector.modifications;

import com.github.javaparser.Position;
import edu.ucr.cs.riple.injector.offsets.FileOffsetStore;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

/** Represents a composite modification that consists of multiple modifications. */
public class MultiPositionModification implements Modification {

  /** The modifications that should be applied to the source file in the reverse order. */
  final SortedSet<Modification> modifications;

  public MultiPositionModification(Set<Modification> modifications) {
    // Modification are sorted to start from the last position, to make the changes ineffective to
    // current computed offsets.
    this.modifications = new TreeSet<>(Collections.reverseOrder());
    this.modifications.addAll(modifications);
  }

  @Override
  public void visit(List<String> lines, FileOffsetStore offsetStore) {
    this.modifications.forEach(modification -> modification.visit(lines, offsetStore));
  }

  @Override
  public Position getStartingPosition() {
    return modifications.last().getStartingPosition();
  }

  @Override
  public int compareTo(@Nonnull Modification o) {
    return COMPARATOR.compare(this, o);
  }

  @Override
  public Set<Modification> flatten() {
    return modifications.stream()
        .flatMap(modification -> modification.flatten().stream())
        .collect(Collectors.toSet());
  }
}
