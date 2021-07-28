package edu.ucr.cs.riple.autofixer.metadata;

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

  List<Usage> getUsage(String method, String className);

  List<FixGraph.Node> getAllNodes();
}
