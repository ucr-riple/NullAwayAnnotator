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

package edu.ucr.cs.riple.core.cache.downstream;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.cache.Impact;

/**
 * Container class for storing overall impact of a fix applied in target module on downstream
 * dependencies. At this moment, only impact of public methods with non-primitive return are stored.
 */
public class DownstreamImpact extends Impact {

  public DownstreamImpact(Report report) {
    super(report.root);
    // Only store impacts of fixes targeting methods.
    Preconditions.checkArgument(
        fix.isOnMethod() || fix.isOnField(),
        "Unexpected Fix instance. Only impacts of fixes on methods / fields should be tracked for downstream dependencies");
    this.triggeredErrors = ImmutableSet.copyOf(report.triggeredErrors);
  }
}
