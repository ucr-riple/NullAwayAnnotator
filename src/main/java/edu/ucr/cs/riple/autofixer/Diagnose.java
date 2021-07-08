package edu.ucr.cs.riple.autofixer;

import static edu.ucr.cs.riple.autofixer.Utility.*;

import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.autofixer.metadata.CallGraph;
import edu.ucr.cs.riple.autofixer.metadata.MethodInheritanceTree;
import edu.ucr.cs.riple.autofixer.metadata.MethodNode;
import edu.ucr.cs.riple.autofixer.nullaway.AutoFixConfig;
import edu.ucr.cs.riple.injector.Fix;
import edu.ucr.cs.riple.injector.Injector;
import edu.ucr.cs.riple.injector.WorkList;
import edu.ucr.cs.riple.injector.WorkListBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Diagnose {

  Injector injector;
  String buildCommand;
  List<DiagnoseReport> finishedReports;
  String fixPath;
  String diagnosePath;
  MethodInheritanceTree methodInheritanceTree;
  CallGraph callGraph;
  Bank bank;
  boolean protectInheritance = false;

  public void start(String buildCommand, String out_dir, boolean optimized) {
    this.buildCommand = buildCommand;
    this.fixPath = out_dir + "/fixes.csv";
    this.diagnosePath = out_dir + "/diagnose.json";
    this.finishedReports = new ArrayList<>();
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
    writeReports(finishedReports);
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
    System.out.println("FIX TYPE IS: " + fix.location);
    List<Fix> suggestedFix = new ArrayList<>();
    suggestedFix.add(fix);
    if (protectInheritance) {
      protectInheritanceRules(fix, suggestedFix);
    }
    injector.start(Collections.singletonList(new WorkList(suggestedFix)));
    DiagnoseReport diagnoseReport;
    if ("METHOD_RETURN".equals(fix.location)) {
      String[] workList = callGraph.getUserClassesOfMethod(fix.method, fix.className);
      if (workList == null) {
        diagnoseReport = new DiagnoseReport(fix, -1);
        finishedReports.add(diagnoseReport);
        return suggestedFix;
      }
      AutoFixConfig.AutoFixConfigWriter writer = new AutoFixConfig.AutoFixConfigWriter()
              .setLogError(true, false)
              .setMakeCallGraph(false)
              .setOptimized(false)
              .setMethodInheritanceTree(false)
              .setSuggest(true)
              .setMakeCallGraph(false)
              .setWorkList(workList);
      writer.write("/tmp/NullAwayFix/explorer.config");
    }
    diagnoseReport = makeReport(fix);
    finishedReports.add(diagnoseReport);
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

