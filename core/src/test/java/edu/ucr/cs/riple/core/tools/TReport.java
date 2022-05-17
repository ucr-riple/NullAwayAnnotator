package edu.ucr.cs.riple.core.tools;

import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.injector.Change;
import edu.ucr.cs.riple.injector.location.Location;

public class TReport extends Report {
  public TReport(Location root, int effect) {
    super(new Fix(new Change(root, "javax.annotation.Nullable", true), null, null, null), effect);
  }
}
