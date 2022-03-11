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

package edu.ucr.cs.riple.core.metadata.trackers;

import edu.ucr.cs.riple.core.FixType;
import edu.ucr.cs.riple.injector.Fix;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CompoundTracker implements UsageTracker {

  private final List<UsageTracker> trackers;

  public CompoundTracker(
      FieldUsageTracker fieldUsageTracker, MethodUsageTracker methodUsageTracker) {
    this.trackers = new ArrayList<>();
    this.trackers.add(fieldUsageTracker);
    this.trackers.add(methodUsageTracker);
    this.trackers.add(
        new UsageTracker() {
          @Override
          public Set<String> getUsers(Fix fix) {
            if (!fix.location.equals(FixType.PARAMETER.name)) {
              return null;
            }
            return Collections.singleton(fix.className);
          }

          @Override
          public Set<Usage> getUsage(Fix fix) {
            if (!fix.location.equals(FixType.PARAMETER.name)) {
              return null;
            }
            return Collections.singleton(new Usage(fix.method, fix.className));
          }
        });
  }

  @Override
  public Set<String> getUsers(Fix fix) {
    for (UsageTracker tracker : this.trackers) {
      Set<String> ans = tracker.getUsers(fix);
      if (ans != null) {
        return ans;
      }
    }
    throw new IllegalStateException("Usage cannot be null at this point." + fix);
  }

  @Override
  public Set<Usage> getUsage(Fix fix) {
    for (UsageTracker tracker : this.trackers) {
      Set<Usage> ans = tracker.getUsage(fix);
      if (ans != null) {
        return ans;
      }
    }
    throw new IllegalStateException("Usage cannot be null at this point." + fix);
  }
}
