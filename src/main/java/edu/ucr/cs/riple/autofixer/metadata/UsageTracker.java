package edu.ucr.cs.riple.autofixer.metadata;

import edu.ucr.cs.riple.injector.Fix;
import java.util.List;
import java.util.Objects;

public interface UsageTracker {
  class Usage {
    public final String method;
    public final String clazz;

    public Usage(String method, String clazz) {
      this.method = method;
      this.clazz = clazz;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Usage)) return false;
      Usage usage = (Usage) o;
      return Objects.equals(method, usage.method) && Objects.equals(clazz, usage.clazz);
    }

    @Override
    public int hashCode() {
      return Objects.hash(method, clazz);
    }

    @Override
    public String toString() {
      return "Usage{" + "method='" + method + '\'' + ", clazz='" + clazz + '\'' + '}';
    }
  }

  List<String> getUsers(Fix fix);

  List<Usage> getUsage(Fix fix);
}
