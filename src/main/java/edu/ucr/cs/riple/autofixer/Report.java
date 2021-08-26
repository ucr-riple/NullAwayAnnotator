package edu.ucr.cs.riple.autofixer;

import edu.ucr.cs.riple.injector.Fix;
import java.util.HashSet;
import java.util.Set;

public class Report {

  public int effectiveNess;
  public Fix fix;
  public Set<Fix> chain;
  public Set<Fix> triggered;

  public Report(Fix fix, int effectiveNess) {
    this.effectiveNess = effectiveNess;
    this.fix = fix;
    this.chain = new HashSet<>();
  }

  public static Report empty(Fix fix) {
    return new Report(fix, 0);
  }

  public void setChain(Set<Fix> chain) {
    this.chain = chain;
  }
}
