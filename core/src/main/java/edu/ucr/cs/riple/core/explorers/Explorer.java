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

import com.uber.nullaway.fixserialization.FixSerializationConfig;
import edu.ucr.cs.riple.core.Annotator;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.metadata.index.Bank;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.FixEntity;
import edu.ucr.cs.riple.injector.Fix;
import java.util.HashSet;
import java.util.Set;

public abstract class Explorer {

  protected final Annotator annotator;
  protected final Bank<Error> errorBank;
  protected final Bank<FixEntity> fixBank;

  public Explorer(Annotator annotator, Bank<Error> errorBank, Bank<FixEntity> fixBank) {
    this.annotator = annotator;
    this.errorBank = errorBank;
    this.fixBank = fixBank;
  }

  public Report effect(Fix fix) {
    FixSerializationConfig.Builder config =
        new FixSerializationConfig.Builder()
            .setSuggest(true, false)
            .setAnnotations(annotator.nullableAnnot, "UNKNOWN")
            .setOutputDirectory(annotator.dir.toString());
    ;
    annotator.buildProject(config);
    if (annotator.errorPath.toFile().exists()) {
      return new Report(fix, errorBank.compare());
    }
    return Report.empty(fix);
  }

  public Report effectByScope(Fix fix, Set<String> workSet) {
    if (workSet == null) {
      workSet = new HashSet<>();
    }
    workSet.add(fix.className);
    FixSerializationConfig.Builder config =
        new FixSerializationConfig.Builder()
            .setSuggest(true, false)
            .setAnnotations(annotator.nullableAnnot, "UNKNOWN")
            .setOutputDirectory(annotator.dir.toString());
    ;
    annotator.buildProject(config);
    if (annotator.errorPath.toFile().exists()) {
      int totalEffect = 0;
      totalEffect += errorBank.compareByClass(fix.className, true).size;
      for (String clazz : workSet) {
        if (clazz.equals(fix.className)) {
          continue;
        }
        totalEffect += errorBank.compareByClass(clazz, false).size;
      }
      return new Report(fix, totalEffect);
    }
    return Report.empty(fix);
  }

  public abstract boolean isApplicable(Fix fix);

  public abstract boolean requiresInjection(Fix fix);
}
