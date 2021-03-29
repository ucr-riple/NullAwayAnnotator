package edu.ucr.cs.riple.diagnose;


import edu.ucr.cs.riple.AutoFix;
import edu.ucr.cs.riple.NullAwayAutoFixExtension;
import edu.ucr.cs.riple.annotationinjector.Fix;
import edu.ucr.cs.riple.annotationinjector.Injector;
import edu.ucr.cs.riple.annotationinjector.WorkList;
import edu.ucr.cs.riple.annotationinjector.WorkListBuilder;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Diagnose extends DefaultTask {

  String executable;
  Injector injector;
  Project project;
  String buildCommand;
  Map<Fix, DiagnoseReport> fixReportMap;
  String fixPath;
  String diagnosePath;

  boolean deep;

  @TaskAction
  public void diagnose() {
    System.out.println("Diagnose Started...");
    NullAwayAutoFixExtension autoFixExtension =
        getProject().getExtensions().findByType(NullAwayAutoFixExtension.class);
    deep = false;
    if (autoFixExtension == null) {
      autoFixExtension = new NullAwayAutoFixExtension();
    }
    DiagnoseExtension diagnoseExtension = getProject().getExtensions().findByType(DiagnoseExtension.class);
    if (diagnoseExtension != null) {
      diagnosePath = diagnoseExtension.getFixPath();
      deep = diagnoseExtension.getDeep();
    } else {
      diagnosePath =
          autoFixExtension.getFixPath() == null
              ? getProject().getProjectDir().getAbsolutePath()
              : autoFixExtension.getFixPath();
      diagnosePath = (diagnosePath.endsWith("/") ? diagnosePath : diagnosePath + "/") + "diagnose.json";
    }

    executable = autoFixExtension.getExecutable();
    injector = Injector.builder().setMode(Injector.MODE.BATCH).setCleanImports(false).build();
    project = getProject();
    detectCommandsAndPaths();
    System.out.println("Requesting preparation");
    prepare(true);
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
          Files.write(path, fileContents);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    if (base == null) {
      return;
    }
    writeReports(base);
  }

  private void analyze(Fix fix) {
    if (!deep) {
      injector.start(Collections.singletonList(new WorkList(Collections.singletonList(fix))));
      fixReportMap.put(fix, makeReport());
      return;
    }
    boolean finished;
    prepare(false);
    List<Fix> baseFixes = readFixes();
    if(baseFixes == null){
      return;
    }

    injector.start(Collections.singletonList(new WorkList(Collections.singletonList(fix))));
    ArrayList<Fix> applied = new ArrayList<>();
    do {
      List<Fix> currentFixes = readFixes();
      if(currentFixes != null) {
        currentFixes.removeAll(baseFixes);
        finished = true;
        for (Fix newFix : currentFixes) {
          applied.add(newFix);
          injector.start(
              Collections.singletonList(new WorkList(Collections.singletonList(newFix))));
          finished = false;
        }
      }else {
        finished = true;
      }
      if(!finished){
        prepare(false);
      }
    } while (!finished);

    DeepReport report = new DeepReport(makeReport().getErrors(), applied);
    fixReportMap.put(fix, report);
  }

  private List<Fix> readFixes(){
    try
    {
      JSONObject obj = (JSONObject) new JSONParser().parse(new FileReader(fixPath));
      JSONArray fixesJson = (JSONArray) obj.get("fixes");
      List<Fix> fixes = new ArrayList<>();
      for (Object o : fixesJson) {
        fixes.add(Fix.createFromJson((JSONObject) o));
      }
      return fixes;
    }catch (Exception e){
      e.printStackTrace();
      return null;
    }
  }

  private void prepare(boolean copy) {
    try {
      System.out.println("Preparing project: " + copy);
      Process p = Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", buildCommand});
      System.out.println("Building with command: " + buildCommand);
      p.waitFor();
      System.out.println("Built.");
      new File(diagnosePath).delete();
      System.out.println("Deleted old diagnose file.");
      if (copy) {
        System.out.println("Making new diagnose.json.");
        Process p2 = Runtime.getRuntime()
            .exec(
                new String[] {
                  "/bin/sh", "-c", "cp " + fixPath + " " + diagnosePath
                });
        p2.waitFor();
        System.out.println("Made.");
      }
      System.out.println("Prepare done");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void writeReports(DiagnoseReport base) {
    JSONObject result = new JSONObject();
    JSONArray reports = new JSONArray();
    for(Fix fix: fixReportMap.keySet()){
      JSONObject report = fix.getJson();
      DiagnoseReport diagnoseReport = fixReportMap.get(fix);
      report.put("jump", diagnoseReport.getErrors().size() - base.getErrors().size());
      JSONArray errors = diagnoseReport.compare(base);
      report.put("errors", errors);
      if(deep){
        report.put("suggested", ((DeepReport) diagnoseReport).getFixes());
      }
      reports.add(report);
    }
    reports.sort((o1, o2) -> {
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

  private void detectCommandsAndPaths() {
    String executablePath = AutoFix.findPathToExecutable(project.getProjectDir().getAbsolutePath(), executable);
    String task = "";
    if (!project.getPath().equals(":")) task = project.getPath() + ":";
    task += "build -x test";
    buildCommand = "cd " + executablePath + " && ./" + executable + " " + task;
    String diagnoseDir = diagnosePath.substring(0, diagnosePath.length() - "diagnose.json".length());
    fixPath = diagnoseDir + "/fixes.json";
  }

  private DiagnoseReport makeReport() {
    try {
      Process proc = Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", buildCommand});
      String line;
      if (proc != null) {
        BufferedReader input = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        final String nullAwayErrorMessagePattern = "error:";
        List<String> errors = new ArrayList<>();
        while ((line = input.readLine()) != null) {
          if (line.contains(nullAwayErrorMessagePattern)) {
            String errorMessage = line.substring(line.indexOf("Away] ") + 6);
            errors.add(errorMessage);
          }
        }
        input.close();
        return new DiagnoseReport(errors);
      }
      return null;
    } catch (Exception e) {
      System.out.println("Error happened: " + e.getMessage());
      throw new RuntimeException("Could not run command: " + buildCommand + " from gradle");
    }
  }
}
