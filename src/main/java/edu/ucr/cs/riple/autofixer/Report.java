package edu.ucr.cs.riple.autofixer;

import edu.ucr.cs.riple.injector.Fix;

public class Report {

  public int effectiveNess;
  public Fix fix;

  public Report(Fix fix, int effectiveNess) {
    this.effectiveNess = effectiveNess;
    this.fix = fix;
  }

  public static Report empty(Fix fix) {
    return new Report(fix, 0);
  }
}
