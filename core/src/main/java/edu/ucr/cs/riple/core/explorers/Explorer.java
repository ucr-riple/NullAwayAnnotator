package edu.ucr.cs.riple.core.explorers;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.AnnotationInjector;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.metadata.index.Bank;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;

public abstract class Explorer {
  protected final AnnotationInjector injector;
  protected final Bank<Error> errorBank;
  protected final Bank<Fix> fixBank;
  protected final ImmutableSet<Report> reports;
  protected final Config config;

  public Explorer(
      AnnotationInjector injector,
      Bank<Error> errorBank,
      Bank<Fix> fixBank,
      ImmutableSet<Fix> fixes,
      Config config) {
    this.injector = injector;
    this.errorBank = errorBank;
    this.fixBank = fixBank;
    this.reports =
        fixes.stream().map(fix -> new Report(fix, -1)).collect(ImmutableSet.toImmutableSet());
    this.config = config;
  }

  protected abstract boolean initializeReports();

  protected abstract void finalizeReports();

  protected abstract void executeNextCycle();

  public ImmutableSet<Report> explore() {
    System.out.println("Max Depth level: " + config.depth);
    for (int i = 0; i < config.depth; i++) {
      System.out.print("Analyzing at level " + (i + 1) + ", ");
      if (!initializeReports()) {
        System.out.println("\nAnalysis finished at this iteration.");
        break;
      }
      executeNextCycle();
      finalizeReports();
    }
    return reports;
  }
}
