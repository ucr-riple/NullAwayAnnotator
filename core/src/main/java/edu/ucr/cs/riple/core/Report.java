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

package edu.ucr.cs.riple.core;

import edu.ucr.cs.riple.core.metadata.index.Fix;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Report {

  public int effect;
  public Fix root;
  public Set<Fix> tree;
  public Set<Fix> triggered;
  public boolean finished;

  public boolean processed;

  public Report(Fix root, int effect) {
    this.effect = effect;
    this.root = root;
    this.tree = new HashSet<>();
    this.finished = false;
    this.triggered = new HashSet<>();
    this.processed = false;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (!(other instanceof Report)) return false;
    Report report = (Report) other;
    return root.equals(report.root);
  }

  @Override
  public int hashCode() {
    return Objects.hash(root);
  }
}
