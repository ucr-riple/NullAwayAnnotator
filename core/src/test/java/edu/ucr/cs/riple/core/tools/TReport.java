package edu.ucr.cs.riple.core.tools;

import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.injector.Change;
import edu.ucr.cs.riple.injector.location.Location;

public class TReport extends Report {

  public final Location root;
  public final int effect;

  public TReport(Location root, int effect) {
    super(new Fix(new Change(root, "javax.annotation.Nullable", true), null, null, null), effect);
    this.root = root;
    this.effect = effect;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TReport)) return false;
    if (!super.equals(o)) return false;
    TReport tReport = (TReport) o;
    return effect == tReport.effect && root.equals(tReport.root);
  }

  @Override
  public String toString() {
    return "TReport{" + "root=" + root + ", effect=" + effect + '}';
  }
}
