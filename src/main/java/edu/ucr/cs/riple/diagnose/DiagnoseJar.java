package edu.ucr.cs.riple.diagnose;

import edu.ucr.cs.riple.annotationinjector.Fix;
import edu.ucr.cs.riple.annotationinjector.Injector;
import edu.ucr.cs.riple.annotationinjector.WorkList;
import edu.ucr.cs.riple.annotationinjector.WorkListBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiagnoseJar {

  Injector injector;
  String buildCommand;
  Map<Fix, DiagnoseReport> fixReportMap;
  String fixPath;
  String diagnosePath;

  private static void executeCommand(String command) {
    try {
      System.out.println("Executing command: " + command);
      Process p = Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", command});
      System.out.println("Requested command: " + command);
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
    this.fixPath = out_dir + "/fixes.json";
    this.diagnosePath = out_dir + "/diagnose.json";
    System.out.println("Diagnose Started...");
    injector = Injector.builder().setMode(Injector.MODE.BATCH).setCleanImports(false).build();
    System.out.println("Requesting preparation");
    prepare(out_dir, optimized);
    System.out.println("Build command: " + buildCommand);
    fixReportMap = new HashMap<>();
    List<WorkList> workListLists = new WorkListBuilder(diagnosePath).getWorkLists();
    DiagnoseReport base = makeReport();
    byte[] fileContents;
    Path path;
    try {
      for (WorkList workList : workListLists) {
        for (Fix fix : workList.getFixes()) {
          path = Paths.get(fix.uri);
          fileContents = Files.readAllBytes(path);
          analyze(fix);
          try (OutputStream out = Files.newOutputStream(path)) {
            int len = fileContents.length;
            int rem = len;
            while (rem > 0) {
              int n = Math.min(rem, 8192);
              out.write(fileContents, (len - rem), n);
              rem -= n;
            }
            out.flush();
            out.close();
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    writeReports(base);
  }
  private void analyze(Fix fix) {
    injector.start(Collections.singletonList(new WorkList(Collections.singletonList(fix))));
    fixReportMap.put(fix, makeReport());
  }

  @SuppressWarnings("Unchecked")
  private void prepare(String out_dir, boolean optimized) {
    try {
      System.out.println("Preparing project: " + true);
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
      System.out.println("Deleted old diagnose file.");
      System.out.println("Making new diagnose.json.");
      if(!optimized){
        executeCommand("cp " + fixPath + " " + diagnosePath);
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
          writer.flush();
          writer.write(toDiagnose.toJSONString());
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

  private void writeReports(DiagnoseReport base) {
    JSONObject result = new JSONObject();
    JSONArray reports = new JSONArray();
    for (Fix fix : fixReportMap.keySet()) {
      JSONObject report = fix.getJson();
      DiagnoseReport diagnoseReport = fixReportMap.get(fix);
      report.put("jump", diagnoseReport.getErrors().size() - base.getErrors().size());
      JSONArray errors = diagnoseReport.compare(base);
      report.put("errors", errors);
      reports.add(report);
    }
    reports.sort(
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
    result.put("reports", reports);
    try (Writer writer =
        Files.newBufferedWriter(
            Paths.get("/tmp/NullAwayFix/diagnose_report.json"), Charset.defaultCharset())) {
      writer.write(result.toJSONString().replace("\\/", "/").replace("\\\\\\", "\\"));
      writer.flush();
    } catch (IOException e) {
      throw new RuntimeException("Could not create the diagnose report json file");
    }
  }

  private DiagnoseReport makeReport() {
    try {
      executeCommand(buildCommand);
      File tempFile = new File("/tmp/NullAwayFix/errors.json");
      boolean exists = tempFile.exists();
      if(exists){
        Object obj = new JSONParser().parse(new FileReader("/tmp/NullAwayFix/errors.json"));
        return new DiagnoseReport((JSONObject)obj);
      }
      return DiagnoseReport.empty();
    } catch (Exception e) {
      System.out.println("Error happened: " + e.getMessage());
      throw new RuntimeException("Could not run command: " + buildCommand);
    }
  }
}

