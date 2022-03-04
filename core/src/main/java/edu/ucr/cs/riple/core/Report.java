package edu.ucr.cs.riple.core;

import edu.ucr.cs.riple.injector.Fix;
import java.util.HashSet;
import java.util.Set;

public class Report {

  public int effectiveNess;
  public Fix fix;
  public Set<Fix> followups;
  public Set<Fix> triggered;
  public boolean finished;

  public Report(Fix fix, int effectiveNess) {
    this.effectiveNess = effectiveNess;
    this.fix = fix;
    this.followups = new HashSet<>();
    this.finished = false;
  }

  public static Report empty(Fix fix) {
    return new Report(fix, 0);
  }
}
