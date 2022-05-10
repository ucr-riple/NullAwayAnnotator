package edu.ucr.cs.riple.core.log;

public class Log {

  int node;
  int requested;
  long time;
  long buildTime = 0;

  private long timer;

  public void reset() {
    this.node = 0;
    this.requested = 0;
    this.time = 0;
    this.buildTime = 0;
    this.timer = 0;
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

  public void startTimer() {
    this.timer = System.currentTimeMillis();
  }

  public void stopTimerAndCapture() {
    this.time += System.currentTimeMillis() - this.timer;
    this.timer = 0;
  }

  public void stopTimerAndCaptureBuildTime() {
    this.buildTime += System.currentTimeMillis() - this.timer;
    this.timer = 0;
  }

  public void incrementBuildRequest() {
    this.requested += 1;
  }

  public void updateNodeNumber(int number) {
    this.node += number;
  }
}
