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
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
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

  private List<Fix> init(String buildCommand) {
    this.buildCommand = buildCommand;
    this.fixPath = out_dir + "/fixes.csv";
    this.diagnosePath = out_dir + "/diagnose.json";
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
    buildProject(config);
    List<Fix> allFixes = Collections.unmodifiableList(Utility.readAllFixes());
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
    return allFixes;
  }

  public void start(String buildCommand, String out_dir, boolean useCache) {
    System.out.println("AutoFixer Started...");
    this.out_dir = out_dir;
    List<Fix> fixes = init(buildCommand);
    if (useCache) {
      removeCachedFixes(fixes, out_dir);
    }
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
          System.out.println("Fix requires injection");
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


  private static void removeCachedFixes(List<Fix> fixes, String out_dir) {
    if (!Files.exists(Paths.get(out_dir + "/reports.json"))) {
      return;
    }
    try {
      System.out.println("Reading cached fixes reports");
      JSONObject cachedObjects =
          (JSONObject) new JSONParser().parse(new FileReader(out_dir + "/reports.json"));
      JSONArray cachedJson = (JSONArray) cachedObjects.get("fixes");
      List<Report> cached = new ArrayList<>();
      for (Object o : cachedJson) {
        JSONObject reportJson = (JSONObject) o;
        int effect = Integer.parseInt(reportJson.get("effect").toString());
        if(effect < 3){
          cached.add(new Report(Fix.createFromJson(reportJson), effect));
        }
      }
      fixes.removeAll(cached.stream().map(report -> report.fix).collect(Collectors.toList()));
    } catch (Exception ignored) { }
    System.out.println("Processing cache fixes finished.");
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
