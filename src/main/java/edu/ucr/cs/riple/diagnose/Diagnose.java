package edu.ucr.cs.riple.diagnose;

import edu.riple.annotationinjector.Fix;
import edu.riple.annotationinjector.Injector;
import edu.riple.annotationinjector.WorkList;
import edu.riple.annotationinjector.WorkListBuilder;
import edu.ucr.cs.riple.AutoFix;
import edu.ucr.cs.riple.NullAwayAutoFixExtension;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

import java.io.BufferedReader;
import java.io.InputStreamReader;
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

  @TaskAction
  public void diagnose() {
    NullAwayAutoFixExtension autoFixExtension =
        getProject().getExtensions().findByType(NullAwayAutoFixExtension.class);
    if (autoFixExtension == null) {
      autoFixExtension = new NullAwayAutoFixExtension();
    }
    executable = autoFixExtension.getExecutable();
    injector =
            Injector.builder()
                    .setMode(Injector.MODE.BATCH)
                    .setCleanImports(false)
                    .setNumberOfWorkers(1)
                    .build();
    project = getProject();
    buildCommand = detectBuildCommand();
    fixReportMap = new HashMap<>();

    String fixPath =
        autoFixExtension.getFixPath() == null
            ? getProject().getProjectDir().getAbsolutePath()
            : autoFixExtension.getFixPath();
    fixPath = (fixPath.endsWith("/") ? fixPath : fixPath + "/") + "fixes.json";

    List<WorkList> workListLists = new WorkListBuilder(fixPath).getWorkLists();

    DiagnoseReport base = makeReport();
    for(WorkList workList: workListLists){
      for(Fix fix: workList.getFixes()){
        injector.start(Collections.singletonList(new WorkList(Collections.singletonList(fix))));
        fixReportMap.put(fix, makeReport());
      }
    }
  }

  private String detectBuildCommand() {
    String executablePath = AutoFix.findPathToExecutable(project.getProjectDir().getAbsolutePath(), executable);
    String task = "";
    if (!project.getPath().equals(":")) task = project.getPath() + ":";
    task += "build -x test";
    return "cd " + executablePath + " && ./" + executable + " " + task;
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


