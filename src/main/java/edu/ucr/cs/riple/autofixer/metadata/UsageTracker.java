package edu.ucr.cs.riple.autofixer.metadata;

import edu.ucr.cs.riple.injector.Fix;
import java.util.List;

public interface UsageTracker {
  class Usage {
    String method;
    String clazz;

    public Usage(String method, String clazz) {
      this.method = method;
      this.clazz = clazz;
    }
  }

  List<Usage> getUsage(Fix fix);
}
