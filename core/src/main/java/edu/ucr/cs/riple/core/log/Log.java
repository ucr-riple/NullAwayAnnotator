package edu.ucr.cs.riple.core.log;

public class Log {

  int total;
  int requested;
  long time;
  long deep;
  long buildTime = 0;

  static final Log instance = new Log();

  @Override
  public String toString() {
    return "total="
        + total
        + ", requested="
        + requested
        + ", time="
        + time
        + ", deep="
        + deep
        + ", buildTime="
        + buildTime;
  }
}
