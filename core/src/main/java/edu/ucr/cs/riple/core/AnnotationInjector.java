package edu.ucr.cs.riple.core;

import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.injector.Change;
import edu.ucr.cs.riple.injector.Injector;
import edu.ucr.cs.riple.injector.WorkListBuilder;
import java.util.Set;
import java.util.stream.Collectors;

public class AnnotationInjector {
  private final Injector injector;

  public AnnotationInjector(Config config) {
    this.injector =
        Injector.builder()
            .setMode(Injector.MODE.BATCH)
            .keepStyle(!config.lexicalPreservationDisabled)
            .build();
  }

  public void removeFixes(Set<Fix> fixes) {
    if (fixes == null || fixes.size() == 0) {
      return;
    }
    Set<Change> toRemove =
        fixes
            .stream()
            .map(fix -> new Change(fix.change.location, fix.annotation, false))
            .collect(Collectors.toSet());
    injector.start(new WorkListBuilder(toRemove).getWorkLists(), false);
  }

  public void injectFixes(Set<Fix> fixes) {
    if (fixes == null || fixes.size() == 0) {
      return;
    }
    injectChanges(fixes.stream().map(fix -> fix.change).collect(Collectors.toSet()));
  }

  public void injectChanges(Set<Change> changes) {
    if (changes == null || changes.size() == 0) {
      return;
    }
    injector.start(new WorkListBuilder(changes).getWorkLists(), false);
  }
}
