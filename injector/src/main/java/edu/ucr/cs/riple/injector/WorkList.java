/*
 * Copyright (c) 2022 University of California, Riverside.
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

package edu.ucr.cs.riple.injector;

import java.util.ArrayList;
import java.util.List;

public class WorkList {
  private final String uri;
  private final List<Fix> fixes;

  public WorkList(String uri) {
    this.uri = uri;
    this.fixes = new ArrayList<>();
  }

  public WorkList(List<Fix> fixes) {
    this.fixes = fixes;
    assert fixes.size() > 0;
    uri = fixes.get(0).uri;
  }

  public void addFix(Fix newFix) {
    for (Fix fix : fixes) if (fix.equals(newFix)) return;
    fixes.add(newFix);
  }

  public List<Fix> getFixes() {
    return fixes;
  }

  public String getUri() {
    return uri;
  }

  public void addContainingAnnotationsToList(List<String> annotsList) {
    for (Fix fix : fixes) if (!annotsList.contains(fix.annotation)) annotsList.add(fix.annotation);
  }

  public String className() {
    return fixes.get(0).className;
  }
}
