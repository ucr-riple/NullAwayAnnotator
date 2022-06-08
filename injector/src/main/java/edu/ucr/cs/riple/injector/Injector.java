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

import java.util.List;

public class Injector {
  public final boolean KEEP;
  public static boolean LOG;

  public Injector(boolean keepStyle) {
    this.KEEP = keepStyle;
  }

  public static InjectorBuilder builder() {
    return new InjectorBuilder();
  }

  public Report start(List<WorkList> workLists, boolean log) {
    LOG = log;
    Report report = new Report();
    for (WorkList workList : workLists) {
      report.totalNumberOfDistinctLocations += workList.getChanges().size();
    }
    report.processed = new Machine(workLists, KEEP).start();
    return report;
  }

  public Report start(List<WorkList> workLists) {
    return start(workLists, false);
  }

  public static class InjectorBuilder {
    private boolean keepStyle = false;

    public InjectorBuilder keepStyle(boolean keep) {
      this.keepStyle = keep;
      return this;
    }

    public Injector build() {
      return new Injector(keepStyle);
    }
  }
}
