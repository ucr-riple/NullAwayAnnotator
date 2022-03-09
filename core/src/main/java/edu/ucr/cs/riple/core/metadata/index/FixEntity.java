package edu.ucr.cs.riple.core.metadata.index;

import edu.ucr.cs.riple.injector.Fix;
import java.util.Objects;

public class FixEntity extends Hashable {

  public final Fix fix;

  public FixEntity(String[] infos) {
    fix = Fix.fromArrayInfo(infos);
    this.clazz = infos[8];
    this.method = infos[9];
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof FixEntity)) return false;
    FixEntity fixEntity = (FixEntity) o;
    return fix.equals(fixEntity.fix);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fix);
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
