package edu.ucr.cs.riple.core.log;

public class Log {

  long node;
  long requested;
  long time;
  long buildTime = 0;

  public void reset() {
    this.node = 0;
    this.requested = 0;
    this.time = 0;
    this.buildTime = 0;
  }

  @Override
  public String toString() {
    return "Total number of nodes="
        + node
        + "\nTotal number of Requested builds="
        + requested
        + "\nTotal time="
        + time
        + "\nTotal time spent on builds="
        + buildTime;
  }

  public long startTimer() {
    return System.currentTimeMillis();
  }

  public void stopTimerAndCapture(long timer) {
    this.time += System.currentTimeMillis() - timer;
  }

  public void stopTimerAndCaptureBuildTime(long timer) {
    this.buildTime += System.currentTimeMillis() - timer;
  }

  public void incrementBuildRequest() {
    this.requested += 1;
  }

  public void updateNodeNumber(long number) {
    this.node += number;
  }
}
