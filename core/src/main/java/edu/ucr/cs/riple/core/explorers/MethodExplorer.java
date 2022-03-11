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

package edu.ucr.cs.riple.core.explorers;

import edu.ucr.cs.riple.core.Annotator;
import edu.ucr.cs.riple.core.FixType;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.metadata.index.Bank;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.FixEntity;
import edu.ucr.cs.riple.injector.Fix;
import java.util.List;

public class MethodExplorer extends AdvancedExplorer {

  public MethodExplorer(
      Annotator annotator, List<Fix> fixes, Bank<Error> errorBank, Bank<FixEntity> fixBank) {
    super(annotator, fixes, errorBank, fixBank, FixType.METHOD);
  }

  @Override
  protected void init() {
    tracker = annotator.methodUsageTracker;
    System.out.println("Trying to find groups for Method Return fixes");
    fixGraph.updateUsages(tracker);
    fixGraph.findGroups();
  }

  @Override
  protected Report effectByScope(Fix fix) {
    return super.effectByScope(fix, tracker.getUsers(fix));
  }

  @Override
  public boolean isApplicable(Fix fix) {
    return fix.location.equals(FixType.METHOD.name);
  }
}
