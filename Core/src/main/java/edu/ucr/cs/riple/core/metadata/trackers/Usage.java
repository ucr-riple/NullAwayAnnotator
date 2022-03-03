package edu.ucr.cs.riple.core.metadata.trackers;

import java.util.Objects;

public class Usage {
  public final String method;
  public final String clazz;

  public Usage(String method, String clazz) {
    this.method = method == null ? "null" : method;
    this.clazz = clazz == null ? "null" : clazz;
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
