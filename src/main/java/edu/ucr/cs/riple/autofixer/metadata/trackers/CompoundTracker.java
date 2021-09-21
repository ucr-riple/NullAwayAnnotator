package edu.ucr.cs.riple.autofixer.metadata.trackers;

import edu.ucr.cs.riple.autofixer.FixType;
import edu.ucr.cs.riple.injector.Fix;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CompoundTracker implements UsageTracker {

  private final List<UsageTracker> trackers;

  public CompoundTracker(FieldUsageTracker fieldUsageTracker, CallUsageTracker callUsageTracker) {
    this.trackers = new ArrayList<>();
    this.trackers.add(fieldUsageTracker);
    this.trackers.add(callUsageTracker);
    this.trackers.add(
        new UsageTracker() {
          @Override
          public Set<String> getUsers(Fix fix) {
            if (!fix.location.equals(FixType.METHOD_PARAM.name)) {
              return null;
            }
            return Collections.singleton(fix.className);
          }

          @Override
          public Set<Usage> getUsage(Fix fix) {
            if (!fix.location.equals(FixType.METHOD_PARAM.name)) {
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
