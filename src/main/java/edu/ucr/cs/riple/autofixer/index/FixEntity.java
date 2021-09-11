package edu.ucr.cs.riple.autofixer.index;

import edu.ucr.cs.riple.injector.Fix;

public class FixEntity extends Hashable {

  public final Fix fix;

  public FixEntity(String[] infos) {
    fix = Fix.fromArrayInfo(infos);
    this.clazz = fix.className;
    this.method = fix.method;
  }
}
