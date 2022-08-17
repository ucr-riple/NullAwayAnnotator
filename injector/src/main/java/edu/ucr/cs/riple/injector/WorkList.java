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

import edu.ucr.cs.riple.injector.changes.Change;
import java.util.ArrayList;
import java.util.List;

public class WorkList {
  private final String uri;
  private final List<Change> changes;

  public WorkList(String uri) {
    this.uri = uri;
    this.changes = new ArrayList<>();
  }

  public void addLocation(Change newLocation) {
    for (Change location : changes) {
      if (location.equals(newLocation)) {
        return;
      }
    }
    changes.add(newLocation);
  }

  public List<Change> getChanges() {
    return changes;
  }

  public String getUri() {
    return uri;
  }

  public void addContainingAnnotationsToList(List<String> annotsList) {
    for (Change location : changes) {
      if (!annotsList.contains(location.annotation)) {
        annotsList.add(location.annotation);
      }
    }
  }

  public String className() {
    return changes.get(0).location.clazz;
  }
}
