package edu.ucr.cs.riple.core;

import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.injector.Injector;
import edu.ucr.cs.riple.injector.Location;
import edu.ucr.cs.riple.injector.WorkListBuilder;
import java.util.Set;
import java.util.stream.Collectors;

public class AnnotationInjector {
  private final Injector injector;

  public AnnotationInjector(Config config) {
    this.injector =
        Injector.builder()
            .setMode(Injector.MODE.BATCH)
            .keepStyle(config.lexicalPreservationEnabled)
            .build();
  }

  public void remove(Set<Fix> fixes) {
    if (fixes == null || fixes.size() == 0) {
      return;
    }
    Set<Location> toRemove =
        fixes
            .stream()
            .map(
                fix ->
                    new Location(
                        fix.annotation,
                        fix.method,
                        fix.variable,
                        fix.kind,
                        fix.clazz,
                        fix.uri,
                        "false"))
            .collect(Collectors.toSet());
    injector.start(new WorkListBuilder(toRemove).getWorkLists(), false);
  }

  public void inject(Set<Fix> fixes) {
    if (fixes == null || fixes.size() == 0) {
      return;
    }
    Set<Location> toApply = fixes.stream().map(fix -> fix.location).collect(Collectors.toSet());
    injector.start(new WorkListBuilder(toApply).getWorkLists(), false);
  }
}
