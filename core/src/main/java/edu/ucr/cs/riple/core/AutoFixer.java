package edu.ucr.cs.riple.core;

import com.google.common.base.Preconditions;
import com.uber.nullaway.fixserialization.FixSerializationConfig;
import edu.ucr.cs.css.Serializer;
import edu.ucr.cs.riple.core.explorers.BasicExplorer;
import edu.ucr.cs.riple.core.explorers.ClassFieldExplorer;
import edu.ucr.cs.riple.core.explorers.DeepExplorer;
import edu.ucr.cs.riple.core.explorers.DummyExplorer;
import edu.ucr.cs.riple.core.explorers.Explorer;
import edu.ucr.cs.riple.core.explorers.MethodParamExplorer;
import edu.ucr.cs.riple.core.explorers.MethodReturnExplorer;
import edu.ucr.cs.riple.core.metadata.index.Bank;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.FixEntity;
import edu.ucr.cs.riple.core.metadata.method.MethodInheritanceTree;
import edu.ucr.cs.riple.core.metadata.trackers.CallUsageTracker;
import edu.ucr.cs.riple.core.metadata.trackers.FieldUsageTracker;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.Fix;
import edu.ucr.cs.riple.injector.Injector;
import edu.ucr.cs.riple.injector.WorkList;
import edu.ucr.cs.riple.injector.WorkListBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AutoFixer {

  public static final String FIXES_NAME = "fixes.tsv";
  public static final String ERROR_NAME = "errors.tsv";

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
    FixSerializationConfig.Builder builder =
        new FixSerializationConfig.Builder()
            .setSuggest(true, true)
            .setAnnotations(AutoFixer.NULLABLE_ANNOT, "UNKNOWN");
    buildProject(builder, false);
    List<Fix> allFixes = Utility.readAllFixes();
    if (useCache) {
      System.out.println("Removing cached fixes");
      Utility.removeCachedFixes(allFixes, out_dir);
    }
    allFixes = Collections.unmodifiableList(allFixes);
    log.total = allFixes.size();
    this.injector = Injector.builder().setMode(Injector.MODE.BATCH).keepStyle(KEEP_STYLE).build();
    this.methodInheritanceTree = new MethodInheritanceTree(Serializer.METHOD_INFO_NAME);
    this.callUsageTracker = new CallUsageTracker(Serializer.CALL_GRAPH_NAME);
    this.fieldUsageTracker = new FieldUsageTracker(Serializer.FIELD_GRAPH_NAME);
    Bank<Error> errorBank = new Bank<>(ERROR_NAME, Error::new);
    Bank<FixEntity> fixBank = new Bank<>(FIXES_NAME, FixEntity::new);
    this.explorers = new ArrayList<>();
    this.deepExplorer = new DeepExplorer(this, errorBank, fixBank);
    if (DEPTH < 0) {
      this.explorers.add(new DummyExplorer(this, null, null));
    } else {
      this.explorers.add(new MethodParamExplorer(this, allFixes, errorBank, fixBank));
      this.explorers.add(new ClassFieldExplorer(this, allFixes, errorBank, fixBank));
      this.explorers.add(new MethodReturnExplorer(this, allFixes, errorBank, fixBank));
      this.explorers.add(new BasicExplorer(this, errorBank, fixBank));
    }
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

  public void buildProject(FixSerializationConfig.Builder writer, boolean count) {
    if (count) {
      log.requested++;
    }
    writer.writeAsXML(out_dir + "/explorer.config");
    try {
      Utility.executeCommand(buildCommand);
    } catch (Exception e) {
      throw new RuntimeException("Could not run command: " + buildCommand);
    }
  }

  public void buildProject(FixSerializationConfig.Builder writer) {
    buildProject(writer, true);
  }
}
