package edu.ucr.cs.riple.autofixer;

import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.autofixer.metadata.CallGraph;
import edu.ucr.cs.riple.autofixer.metadata.MethodInheritanceTree;
import edu.ucr.cs.riple.autofixer.metadata.MethodNode;
import edu.ucr.cs.riple.autofixer.nullaway.FixDisplay;
import edu.ucr.cs.riple.injector.Fix;
import edu.ucr.cs.riple.injector.Injector;
import edu.ucr.cs.riple.injector.WorkList;
import edu.ucr.cs.riple.injector.WorkListBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Diagnose {

  Injector injector;
  String buildCommand;
  List<DiagnoseReport> finishedReports;
  String fixPath;
  String diagnosePath;
  MethodInheritanceTree methodInheritanceTree;
  CallGraph callGraph;
  boolean protectInheritance = false;
  Bank bank;

  @SuppressWarnings("StatementWithEmptyBody")
  private static void executeCommand(String command) {
    try {
      System.out.println("Executing command: " + command);
      Process p = Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", command});
      System.out.println("Requested");
      BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
      while ((reader.readLine()) != null) {}
      p.waitFor();
      System.out.println("Finished.");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void start(String buildCommand, String out_dir, boolean optimized) {
    this.buildCommand = buildCommand;
    this.fixPath = out_dir + "/fixes.csv";
    this.diagnosePath = out_dir + "/diagnose.json";
    bank = new Bank();
    bank.setup();
    methodInheritanceTree = new MethodInheritanceTree(out_dir + "/method_info.csv");
    callGraph = new CallGraph(out_dir + "/call_graph.csv");
    System.out.println("Diagnose Started...");
    injector = Injector.builder().setMode(Injector.MODE.BATCH).build();
    System.out.println("Starting preparation");
    prepare(out_dir, optimized);
    System.out.println("Build command: " + buildCommand);
    List<WorkList> workListLists = new WorkListBuilder(diagnosePath).getWorkLists();
    try {
      for (WorkList workList : workListLists) {
        for (Fix fix : workList.getFixes()) {
          if(finishedReports.stream().anyMatch(diagnoseReport -> diagnoseReport.fix.equals(fix))){
            continue;
          }
          List<Fix> appliedFixes = analyze(fix);
          remove(appliedFixes);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    writeReports();
  }

  private void remove(List<Fix> fixes) {
    List<Fix> toRemove = new ArrayList<>();
    for(Fix fix: fixes){
      Fix removeFix = new Fix(fix.annotation, fix.method, fix.param, fix.location, fix.className, fix.pkg, fix.uri, "false");
      toRemove.add(removeFix);
    }
    injector.start(Collections.singletonList(new WorkList(toRemove)));
  }

  private List<Fix> analyze(Fix fix) {
    List<Fix> suggestedFix = new ArrayList<>();
    suggestedFix.add(fix);
    if(protectInheritance){
      protectInheritanceRules(fix, suggestedFix);
    }
    injector.start(Collections.singletonList(new WorkList(suggestedFix)));
    finishedReports.add(makeReport(fix));
    return suggestedFix;
  }

  private void protectInheritanceRules(Fix fix, List<Fix> suggestedFix) {
        if(fix.location.equals("METHOD_PARAM")) {
      List<MethodNode> subMethods = methodInheritanceTree.getSubMethods(fix.method, fix.className);
      for (MethodNode info : subMethods) {
        suggestedFix.add(new Fix(
                fix.annotation,
                info.method,
                fix.param,
                fix.location,
                info.clazz,
                fix.pkg,
                info.uri,
                fix.inject
        ));
      }
    }
    if(fix.location.equals("METHOD_RETURN")) {
      List<MethodNode> subMethods = methodInheritanceTree.getSuperMethods(fix.method, fix.className);
      for (MethodNode info : subMethods) {
        suggestedFix.add(new Fix(
                fix.annotation,
                info.method,
                fix.param,
                fix.location,
                info.clazz,
                fix.pkg,
                info.uri,
                fix.inject
        ));
      }
    }
  }

  @SuppressWarnings("ALL")
  private void prepare(String out_dir, boolean optimized) {
    try {
      System.out.println("Preparing project: with optimization flag:" + optimized);
      executeCommand(buildCommand);
      if(! new File(fixPath).exists()){
        JSONObject toDiagnose = new JSONObject();
        toDiagnose.put("fixes", new JSONArray());
        FileWriter writer = new FileWriter(diagnosePath);
        writer.write(toDiagnose.toJSONString());
        writer.flush();
        System.out.println("No new fixes from NullAway, created empty list.");
        return;
      }
      new File(diagnosePath).delete();
      convertCSVToJSON(out_dir + "/fixes.csv", out_dir + "/fixes.json");
      System.out.println("Deleted old diagnose file.");
      System.out.println("Making new diagnose.json.");
      if(!optimized){
        executeCommand("cp " + fixPath + " " + diagnosePath);
        convertCSVToJSON(diagnosePath, diagnosePath);
      }else{
        try{
          System.out.println("Removing already diagnosed fixes...");
          Object obj = new JSONParser().parse(new FileReader(fixPath));
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
        }catch (RuntimeException exception){
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

  @SuppressWarnings("ALL")
  private void convertCSVToJSON(String csvPath, String jsonPath) {
    JSONArray fixes = new JSONArray();
    BufferedReader reader;
    FileWriter writer;
    try {
      reader = Files.newBufferedReader(Paths.get(csvPath), Charset.defaultCharset());
      String line = reader.readLine();
      if(line != null) line = reader.readLine();
      while (line != null) {
        FixDisplay fix = FixDisplay.fromCSVLine(line);
        fixes.add(fix.getJson());
        line = reader.readLine();
      }
      reader.close();
      JSONObject res = new JSONObject();
      JSONArray fixesArray = new JSONArray();
      fixesArray.addAll(fixes);
      res.put("fixes", fixesArray);
      writer = new FileWriter(jsonPath);
      writer.write(res.toJSONString().replace("\\/", "/").replace("\\\\\\", "\\"));
      writer.flush();
    } catch (IOException e) {
      System.err.println("Error happened in converting csv to json!");
    }
  }

  @SuppressWarnings("ALL")
  private void writeReports() {
    JSONObject result = new JSONObject();
    JSONArray reportsJson = new JSONArray();
    for (DiagnoseReport report : finishedReports) {
      JSONObject reportJson = report.fix.getJson();
      reportJson.put("jump", report.effectiveNess);
      reportsJson.add(reportJson);
    }
    reportsJson.sort(
        (o1, o2) -> {
          Integer first = (Integer) ((JSONObject) o1).get("jump");
          Integer second = (Integer) ((JSONObject) o2).get("jump");
          if (first.equals(second)) {
            return 0;
          }
          if (first < second) {
            return 1;
          }
          return -1;
        });
    result.put("reports", reportsJson);
    try  {
      FileWriter writer = new FileWriter("/tmp/NullAwayFix/diagnose_report.json");
      writer.write(result.toJSONString().replace("\\/", "/").replace("\\\\\\", "\\"));
      writer.flush();
    } catch (IOException e) {
      throw new RuntimeException("Could not create the diagnose report json file");
    }
  }

  private DiagnoseReport makeReport(Fix fix) {
    try {
      executeCommand(buildCommand);
      File tempFile = new File("/tmp/NullAwayFix/errors.csv");
      boolean exists = tempFile.exists();
      if(exists){
        return new DiagnoseReport(fix, bank.compare());
      }
      return DiagnoseReport.empty(fix);
    } catch (Exception e) {
      System.out.println("Error happened: " + e.getMessage());
      throw new RuntimeException("Could not run command: " + buildCommand);
    }
  }
}

