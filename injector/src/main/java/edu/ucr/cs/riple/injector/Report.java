package edu.ucr.cs.riple.injector;

public class Report {
  public int processed = 0;
  public int totalNumberOfDistinctFixes = 0;

  @Override
  public String toString() {
    return "Total number of distinct fixes: "
        + totalNumberOfDistinctFixes
        + ", and applied "
        + processed
        + " number of them.";
  }
}
