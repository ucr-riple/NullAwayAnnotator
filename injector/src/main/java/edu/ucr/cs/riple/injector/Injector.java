package edu.ucr.cs.riple.injector;

import java.util.List;

public class Injector {
  public final MODE mode;
  public final boolean KEEP;
  public static boolean LOG;

  public enum MODE {
    BATCH,
    TEST
  }

  public Injector(MODE mode, boolean keepStyle) {
    this.mode = mode;
    this.KEEP = keepStyle;
  }

  public static InjectorBuilder builder() {
    return new InjectorBuilder();
  }

  public Report start(List<WorkList> workLists, boolean log) {
    LOG = log;
    Report report = new Report();
    for (WorkList workList : workLists) {
      report.totalNumberOfDistinctFixes += workList.getFixes().size();
    }
    report.processed = new InjectorMachine(workLists, mode, KEEP).start();
    return report;
  }

  public Report start(List<WorkList> workLists) {
    return start(workLists, false);
  }

  public static class InjectorBuilder {
    private MODE mode = MODE.BATCH;
    private boolean keepStyle = false;

    public InjectorBuilder setMode(MODE mode) {
      this.mode = mode;
      return this;
    }

    public InjectorBuilder keepStyle(boolean keep) {
      this.keepStyle = keep;
      return this;
    }

    public Injector build() {
      return new Injector(mode, keepStyle);
    }
  }
}
