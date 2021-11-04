package edu.ucr.cs.riple.autofixer;

import com.google.common.base.Preconditions;
import edu.ucr.cs.riple.autofixer.explorers.BasicExplorer;
import edu.ucr.cs.riple.autofixer.explorers.ClassFieldExplorer;
import edu.ucr.cs.riple.autofixer.explorers.DeepExplorer;
import edu.ucr.cs.riple.autofixer.explorers.Explorer;
import edu.ucr.cs.riple.autofixer.explorers.MethodParamExplorer;
import edu.ucr.cs.riple.autofixer.explorers.MethodReturnExplorer;
import edu.ucr.cs.riple.autofixer.metadata.index.Bank;
import edu.ucr.cs.riple.autofixer.metadata.index.Error;
import edu.ucr.cs.riple.autofixer.metadata.index.FixEntity;
import edu.ucr.cs.riple.autofixer.metadata.method.MethodInheritanceTree;
import edu.ucr.cs.riple.autofixer.metadata.trackers.CallUsageTracker;
import edu.ucr.cs.riple.autofixer.metadata.trackers.FieldUsageTracker;
import edu.ucr.cs.riple.autofixer.nullaway.AutoFixConfig;
import edu.ucr.cs.riple.autofixer.nullaway.Writer;
import edu.ucr.cs.riple.autofixer.util.Utility;
import edu.ucr.cs.riple.injector.Fix;
import edu.ucr.cs.riple.injector.Injector;
import edu.ucr.cs.riple.injector.WorkList;
import edu.ucr.cs.riple.injector.WorkListBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AutoFixer {

  public static String NULLABLE_ANNOT;
  public static int DEPTH;
  public static boolean KEEP_STYLE;

  private String out_dir;
  private String buildCommand;
  private Injector injector;
  private List<Report> finishedReports;
  private List<Explorer> explorers;
  private DeepExplorer deepExplorer;

  public CallUsageTracker callUsageTracker;
  public FieldUsageTracker fieldUsageTracker;
  public MethodInheritanceTree methodInheritanceTree;

  private static final Log log = new Log();

  public static class Log {
    int total;
    int requested;
    long time;
    long deep;

    @Override
    public String toString() {
      return "total=" + total + ", requested=" + requested + ", time=" + time + ", deep=" + deep;
    }
  }

  private List<Fix> init(String buildCommand, boolean useCache) {
    System.out.println("Initializing");
    this.buildCommand = buildCommand;
    this.finishedReports = new ArrayList<>();
    AutoFixConfig.AutoFixConfigWriter config =
        new AutoFixConfig.AutoFixConfigWriter()
            .setLogError(true, true)
            .setMakeCallGraph(true)
            .setMakeFieldGraph(true)
            .setMethodInheritanceTree(true)
            .setSuggest(true, true)
            .setAnnots(AutoFixer.NULLABLE_ANNOT, "UNKNOWN")
            .setWorkList(Collections.singleton("*"));
    buildProject(config, false);
    List<Fix> allFixes = Utility.readAllFixes();
    if (useCache) {
      System.out.println("Removing cached fixes");
      Utility.removeCachedFixes(allFixes, out_dir);
    }
    allFixes = Collections.unmodifiableList(allFixes);
    log.total = allFixes.size();
    this.injector = Injector.builder().setMode(Injector.MODE.BATCH).keepStyle(KEEP_STYLE).build();
    this.methodInheritanceTree = new MethodInheritanceTree(Writer.METHOD_INFO);
    this.callUsageTracker = new CallUsageTracker(Writer.CALL_GRAPH);
    this.fieldUsageTracker = new FieldUsageTracker(Writer.FIELD_GRAPH);
    this.explorers = new ArrayList<>();
    Bank<Error> errorBank = new Bank<>(Writer.ERROR, Error::new);
    Bank<FixEntity> fixBank = new Bank<>(Writer.SUGGEST_FIX, FixEntity::new);
    this.deepExplorer = new DeepExplorer(this, errorBank, fixBank);
    this.explorers.add(new MethodParamExplorer(this, allFixes, errorBank, fixBank));
    this.explorers.add(new ClassFieldExplorer(this, allFixes, errorBank, fixBank));
    this.explorers.add(new MethodReturnExplorer(this, allFixes, errorBank, fixBank));
    this.explorers.add(new BasicExplorer(this, errorBank, fixBank));
    return allFixes;
  }

  public void start(String buildCommand, String out_dir, boolean useCache) {
    log.time = System.currentTimeMillis();
    System.out.println("AutoFixer Started.");
    this.out_dir = out_dir;
    List<Fix> fixes = init(buildCommand, useCache);
    List<WorkList> workListLists = new WorkListBuilder(fixes).getWorkLists();
    try {
      for (WorkList workList : workListLists) {
        for (Fix fix : workList.getFixes()) {
          if (finishedReports.stream().anyMatch(diagnoseReport -> diagnoseReport.fix.equals(fix))) {
            continue;
          }
          List<Fix> appliedFixes = analyze(fix);
          remove(appliedFixes);
        }
      }
      log.deep = System.currentTimeMillis();
      deepExplorer.start(finishedReports);
      log.deep = System.currentTimeMillis() - log.deep;
    } catch (Exception e) {
      e.printStackTrace();
    }
    log.time = System.currentTimeMillis() - log.time;
    Utility.writeReports(finishedReports);
    Utility.writeLog(log);
  }

  public void remove(List<Fix> fixes) {
    if (fixes == null || fixes.size() == 0) {
      return;
    }
    List<Fix> toRemove =
        fixes
            .stream()
            .map(
                fix ->
                    new Fix(
                        fix.annotation,
                        fix.method,
                        fix.param,
                        fix.location,
                        fix.className,
                        fix.uri,
                        "false"))
            .collect(Collectors.toList());
    apply(toRemove);
  }

  public void apply(List<Fix> fixes) {
    if (fixes == null || fixes.size() == 0) {
      return;
    }
    injector.start(new WorkListBuilder(fixes).getWorkLists(), false);
  }

  private List<Fix> analyze(Fix fix) {
    List<Fix> suggestedFix = new ArrayList<>();
    Report report = null;
    for (Explorer explorer : explorers) {
      if (explorer.isApplicable(fix)) {
        if (explorer.requiresInjection(fix)) {
          suggestedFix.add(fix);
          apply(suggestedFix);
        }
        report = explorer.effect(fix);
        break;
      }
    }
    Preconditions.checkNotNull(report);
    finishedReports.add(report);
    return suggestedFix;
  }

  public void buildProject(AutoFixConfig.AutoFixConfigWriter writer, boolean count) {
    if (count) {
      log.requested++;
    }
    writer.write(out_dir + "/explorer.config");
    try {
      Utility.executeCommand(buildCommand);
    } catch (Exception e) {
      throw new RuntimeException("Could not run command: " + buildCommand);
    }
  }

  public void buildProject(AutoFixConfig.AutoFixConfigWriter writer) {
    buildProject(writer, true);
  }
}
