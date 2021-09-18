package edu.ucr.cs.riple.autofixer;

import edu.ucr.cs.riple.autofixer.metadata.index.Error;
import edu.ucr.cs.riple.injector.Fix;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Report {

  public int effectiveNess;
  public Fix fix;
  public Set<Fix> followups;
  public Set<Fix> triggered;
  public List<Error> newErrors;

  public Report(Fix fix, int effectiveNess) {
    this.effectiveNess = effectiveNess;
    this.fix = fix;
    this.followups = new HashSet<>();
  }

  public static Report empty(Fix fix) {
    return new Report(fix, 0);
  }

  public void setFollowups(Set<Fix> followups) {
    this.followups = followups;
  }
}
