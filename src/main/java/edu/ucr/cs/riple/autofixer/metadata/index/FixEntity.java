package edu.ucr.cs.riple.autofixer.metadata.index;

import edu.ucr.cs.riple.injector.Fix;

public class FixEntity extends Hashable {

  public final Fix fix;

  public FixEntity(String[] infos) {
    fix = Fix.fromArrayInfo(infos);
    this.clazz = infos[11];
    this.method = infos[12];
  }

  @Override
  public String toString() {
    return "FixEntity{"
        + "fix="
        + fix
        + ", clazz='"
        + clazz
        + '\''
        + ", method='"
        + method
        + '\''
        + '}';
  }
}
