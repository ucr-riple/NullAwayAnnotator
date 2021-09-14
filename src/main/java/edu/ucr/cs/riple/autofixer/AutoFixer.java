package edu.ucr.cs.riple.autofixer;

import static edu.ucr.cs.riple.autofixer.util.Utility.*;

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
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class AutoFixer {

  public static String NULLABLE_ANNOT;
  public static int DEPTH;

  private String out_dir;
  private String buildCommand;
  private String fixPath;
  private String diagnosePath;
  private Injector injector;
  private List<Report> finishedReports;
  private List<Explorer> explorers;
  private DeepExplorer deepExplorer;

  public CallUsageTracker callUsageTracker;
  public FieldUsageTracker fieldUsageTracker;
  public MethodInheritanceTree methodInheritanceTree;

  private void init(String buildCommand) {
    this.buildCommand = buildCommand;
    this.fixPath = out_dir + "/fixes.csv";
    this.diagnosePath = out_dir + "/diagnose.json";
    this.finishedReports = new ArrayList<>();
    AutoFixConfig.AutoFixConfigWriter config =
        new AutoFixConfig.AutoFixConfigWriter()
            .setLogError(true, true)
            .setMakeCallGraph(true)
            .setMakeFieldGraph(true)
            .setOptimized(true)
            .setMethodInheritanceTree(true)
            .setSuggest(true, false)
            .setAnnots(AutoFixer.NULLABLE_ANNOT, "UNKNOWN")
            .setWorkList(Collections.singleton("*"));
    buildProject(config);
    List<Fix> allFixes = Utility.readAllFixes();
    this.injector = Injector.builder().setMode(Injector.MODE.BATCH).build();
    this.methodInheritanceTree = new MethodInheritanceTree(Writer.METHOD_INFO);
    this.callUsageTracker = new CallUsageTracker(Writer.CALL_GRAPH);
    this.fieldUsageTracker = new FieldUsageTracker(Writer.FIELD_GRAPH);
    this.explorers = new ArrayList<>();
    Bank<Error> errorBank = new Bank<>(Writer.ERROR, Error::new);
    Bank<FixEntity> fixIndex = new Bank<>(Writer.SUGGEST_FIX, FixEntity::new);
    this.deepExplorer = new DeepExplorer(this, errorBank, fixIndex);
    this.explorers.add(new MethodParamExplorer(this, allFixes, errorBank, fixIndex));
    this.explorers.add(new ClassFieldExplorer(this, allFixes, errorBank, fixIndex));
    this.explorers.add(new MethodReturnExplorer(this, allFixes, errorBank, fixIndex));
    this.explorers.add(new BasicExplorer(this, errorBank, fixIndex));
  }

  public void start(String buildCommand, String out_dir, boolean optimized) {
    System.out.println("AutoFixer Started...");
    this.out_dir = out_dir;
    init(buildCommand);
    System.out.println("Starting preparation");
    prepare(out_dir, optimized);
    List<WorkList> workListLists = new WorkListBuilder(diagnosePath).getWorkLists();
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
      deepExplorer.start(finishedReports);
    } catch (Exception e) {
      e.printStackTrace();
    }
    writeReports(finishedReports);
  }

  private List<Fix> analyze(Fix fix) {
    System.out.println("Fix Type: " + fix.location);
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
    injector.start(new WorkListBuilder(fixes).getWorkLists(), true);
  }

  @SuppressWarnings("ALL")
  private void prepare(String out_dir, boolean optimized) {
    try {
      System.out.println("Preparing project: with optimization flag:" + optimized);
      AutoFixConfig.AutoFixConfigWriter config =
          new AutoFixConfig.AutoFixConfigWriter()
              .setLogError(true, false)
              .setMakeCallGraph(false)
              .setMakeFieldGraph(false)
              .setOptimized(false)
              .setMethodInheritanceTree(false)
              .setSuggest(true, false)
              .setAnnots(AutoFixer.NULLABLE_ANNOT, "UNKNOWN")
              .setWorkList(Collections.singleton("*"));
      buildProject(config);
      if (!new File(fixPath).exists()) {
        JSONObject toDiagnose = new JSONObject();
        toDiagnose.put("fixes", new JSONArray());
        FileWriter writer = new FileWriter(diagnosePath);
        writer.write(toDiagnose.toJSONString());
        writer.flush();
        System.out.println("No new fixes from NullAway, created empty list.");
        return;
      }
      new File(diagnosePath).delete();
      convertCSVToJSON(this.fixPath, out_dir + "/fixes.json");
      System.out.println("Deleted old diagnose file.");
      System.out.println("Making new diagnose.json.");
      if (!optimized) {
        executeCommand("cp " + this.fixPath + " " + this.diagnosePath);
        convertCSVToJSON(diagnosePath, diagnosePath);
      } else {
        try {
          System.out.println("Removing already diagnosed fixes...");
          Object obj = new JSONParser().parse(new FileReader(out_dir + "/fixes.json"));
          JSONObject fixes = (JSONObject) obj;
          obj = new JSONParser().parse(new FileReader(out_dir + "/diagnosed.json"));
          JSONObject diagnosed = (JSONObject) obj;
          JSONArray fixes_array = (JSONArray) fixes.get("fixes");
          JSONArray diagnosed_array = (JSONArray) diagnosed.get("fixes");
          fixes_array.removeAll(diagnosed_array);
          JSONObject toDiagnose = new JSONObject();
          toDiagnose.put("fixes", fixes_array);
          FileWriter writer = new FileWriter(diagnosePath);
          writer.write(toDiagnose.toJSONString());
          writer.flush();
        } catch (RuntimeException exception) {
          System.out.println("Exception happened while optimizing suggested fixes.");
          System.out.println("Continuing...");
          executeCommand("cp " + fixPath + " " + diagnosePath);
        }
      }
      System.out.println("Made.");
      System.out.println("Preparation done");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void buildProject(AutoFixConfig.AutoFixConfigWriter writer) {
    writer.write(out_dir + "/explorer.config");
    try {
      executeCommand(buildCommand);
    } catch (Exception e) {
      throw new RuntimeException("Could not run command: " + buildCommand);
    }
  }
}
