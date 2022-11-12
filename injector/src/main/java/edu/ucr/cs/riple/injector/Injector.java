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

import edu.ucr.cs.riple.injector.changes.AddAnnotation;
import edu.ucr.cs.riple.injector.changes.RemoveAnnotation;
import java.util.List;
import java.util.Set;

public class Injector {
  public final boolean preserveStyle;
  public boolean log;

  public Injector(boolean keepStyle) {
    this.preserveStyle = keepStyle;
  }

  public static InjectorBuilder builder() {
    return new InjectorBuilder();
  }

  public Report start(List<WorkList> workLists, boolean log) {
    // Start method does not support addition and deletion on same element. Should be split into
    // call for addition and deletion separately.
    this.log = log;
    Report report = new Report();
    for (WorkList workList : workLists) {
      report.totalNumberOfDistinctLocations += workList.getChanges().size();
    }
    report.processed = new Machine(workLists, preserveStyle, log).start();
    return report;
  }

  public Report addAnnotations(Set<AddAnnotation> requests) {
    return this.start(new WorkListBuilder<>(requests).getWorkLists(), false);
  }

  public Report removeAnnotations(Set<RemoveAnnotation> requests) {
    return this.start(new WorkListBuilder<>(requests).getWorkLists(), false);
  }

  public static class InjectorBuilder {
    private boolean keepStyle = false;

    public InjectorBuilder preserveStyle(boolean keep) {
      this.keepStyle = keep;
      return this;
    }

    public Injector build() {
      return new Injector(keepStyle);
    }
  }
}
